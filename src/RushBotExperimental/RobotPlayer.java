package RushBotExperimental;

import battlecode.common.*;
import battlecode.world.Flag;
import battlecode.world.Trap;

import java.awt.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.ToIntBiFunction;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Random rng;
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    static MapLocation centre;
    static int movesLeft = 0;
    static MapLocation target;
    static int team = 0;

    /**
     * ID compression system.
     * Each robot starts with an ID, theoretically in the range [10000, 14096).
     * ids is an int array which stores the actual ID of each robot.
     * idx is a compressed mapping of each ID to its index in ids
     * ids is sorted by turn order.
     */
    static int[] ids = new int[50];
    static int[] idx = new int[10000];
    /**
     * 0 = BFS bot, receives information from other bots and runs a BFS in idle turns.
     */
    static int selfIdx = -1;

    static Direction[] stack = new Direction[10];
    static int stackSize = 0;
    static int turnDir = 0;
    static RobotInfo[] nearbyAllies;

    // 2d array storing the board
    // 0 = undiscovered
    // 1 = emtpy passable tile
    // 2 = wall
    // 3 = our spawn zone
    // 4 = opponent's spawn zone
    // 5 = dam
    static int[][] board;
    static boolean vertical = true, horizontal = true, rotational = true;
    public enum Symmetry {
        UNKNOWN("Unknown"),
        VERTICAL("Vertical"),
        HORIZONTAL("Horizontal"),
        ROTATIONAL("Rotational"),
        ALL("All");

        public final String label;

        Symmetry(String label) {
            this.label = label;
        }
    }
    static Symmetry symmetry = Symmetry.UNKNOWN;
    static boolean justUpdated = false;

    static int max(int a, int b) {
        if (a > b) {
            return a;
        }
        return b;
    }
    static int min(int a, int b) {
        if (a < b) {
            return a;
        }
        return b;
    }

    static MapLocation closest(MapLocation[] locs) {
        int mn = 1000000000;
        MapLocation res = locs[0];
        for (int i = 0; i < locs.length; i++) {
            int dist = locs[i].distanceSquaredTo(rc.getLocation());
            if (dist < mn) {
                mn = dist;
                res = locs[i];
            }
        }
        return res;
    }

    static void moveBetter(MapLocation pos) throws GameActionException {
        if(stackSize != 0 && (!rc.getLocation().directionTo(pos).equals(stack[0]) || rng.nextInt(8) == 0)) {
            stackSize = 0;
        }
        if(stackSize == 0) {
            stack[stackSize++] = rc.getLocation().directionTo(pos);
        }
        if(stackSize == 1) {
            turnDir = rng.nextInt(2);
        }
        if(stackSize >= 2 && rc.canMove(stack[stackSize - 2])) {
            stackSize--;
        }
        boolean moveCooldownDone = rc.getMovementCooldownTurns() == 0;
        MapLocation nextLoc;
        RobotInfo nextLocRobot;
        boolean triedOtherDir = false;
        boolean hasFlag = rc.hasFlag() || rc.canPickupFlag(rc.getLocation());
        while(stackSize < 8) {
            nextLoc = rc.getLocation().add(stack[stackSize - 1]);
            if(rc.onTheMap(nextLoc)) {
                if(!moveCooldownDone) {
                    // if it's not a wall, and if there's water we can fill it
                    if (!rc.senseMapInfo(nextLoc).isWall() && (!rc.senseMapInfo(nextLoc).isWater() || !hasFlag)) {
                        break;
                    }
                } else {
                    // if we can move there, or if we can fill it
                    if (rc.canMove(stack[stackSize - 1]) || (rc.senseMapInfo(nextLoc).isWater() && !hasFlag)) {
                        break;
                    }
                }
                nextLocRobot = rc.senseRobotAtLocation(nextLoc);
                // otherwise, if we have the flag and the square we're trying to move to is obstructed by a friend
                if (hasFlag && nextLocRobot != null && nextLocRobot.getTeam() == rc.getTeam()) {
                    rc.setIndicatorString("Passing bread to a friend!");
                    break;
                }
            } else {
                // reset if hugging wall, try other turn dir
                stackSize = 1;
                if(triedOtherDir) {
                    break;
                }
                turnDir = 1 - turnDir;
                triedOtherDir = true;
            }
            stack[stackSize] = turnDir == 0 ? stack[stackSize - 1].rotateLeft() : stack[stackSize - 1].rotateRight();
            stackSize++;
        }
        if (stackSize >= 8) {
            stackSize = 1;
        }
        Direction dir = stack[stackSize - 1];
        nextLoc = rc.getLocation().add(dir);
        nextLocRobot = rc.senseRobotAtLocation(nextLoc);
        if (rc.canFill(nextLoc) && !hasFlag) {
            rc.fill(nextLoc);
        }
        if(rc.canMove(dir)) {
            if (rc.hasFlag() && rc.canDropFlag(rc.getLocation().add(dir))) {
                rc.dropFlag(rc.getLocation().add(dir));
            }
            rc.move(dir);
        }
        if(rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.getTeam() == rc.getTeam()) {
            rc.dropFlag(nextLoc);
            writeStack();
        }
    }
    static int stackPassIndex = 3;
    static void writeStack() throws GameActionException {
        // 3 bits for stack size, 1 bit for turn dir, 3 bits for first dir;
        if(stackSize == 0) {
            rc.writeSharedArray(stackPassIndex, 0);
            return;
        }
        for(int i = 0; i < 8; i++) {
            if(stack[0] == Direction.values()[i]) {
                rc.writeSharedArray(stackPassIndex, (stackSize << 4) + (turnDir << 3) + i);
                return;
            }
        }
    }
    static void readStack() throws GameActionException {
        int data = rc.readSharedArray(stackPassIndex);
        if(data == 0) {
            return;
        }
        stackSize = data >> 4;
        turnDir = (data >> 3) & 1;
        stack[0] = Direction.values()[data & 7];
        for(int i = 1; i < stackSize; i++) {
            stack[i] = turnDir == 0 ? stack[i - 1].rotateLeft() : stack[i - 1].rotateRight();
        }
        rc.writeSharedArray(stackPassIndex, 0);
    }

    static void broadcastSwarmTarget(MapLocation loc) throws GameActionException {
        rc.writeSharedArray(0, rc.getID());
        rc.writeSharedArray(1, loc.x);
        rc.writeSharedArray(2, loc.y);
    }

    static MapLocation swarmTarget;
    static int swarmEnd = 0;
    static MapLocation findTarget() throws GameActionException {
        // Targeting algorithm:
        // If we have the flag, go home
        // Chase opponent flag bearers (and call the swarm for help)
        // Go for flags
        // Protect flag bearers
        // If in combat, kite enemies
        // If swarm is active, go to swarm target
        // If we see many allies, activate swarm
        // Go for crumbs
        // Go to the centre
        int swarmLeader = rc.readSharedArray(0);
        if(swarmLeader == rc.getID()) {
            rc.writeSharedArray(0, 0);
        }
        if(rc.hasFlag() || rc.canPickupFlag(rc.getLocation())) return closest(rc.getAllySpawnLocations());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        if(friendlyFlags.length > 0 && rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) {
            for(FlagInfo f : friendlyFlags) {
                if(rc.senseNearbyRobots(f.getLocation(), 0, rc.getTeam().opponent()).length > 0) {
                    broadcastSwarmTarget(f.getLocation());
                    return f.getLocation();
                }
            }
            return friendlyFlags[0].getLocation();
        }
        FlagInfo[] possibleFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        MapLocation[] flagLocs = new MapLocation[possibleFlags.length];
        for(int i = 0; i < possibleFlags.length; i++) {
            flagLocs[i] = possibleFlags[i].getLocation();
        }
        if(possibleFlags.length >= 1) return closest(flagLocs);
        for(RobotInfo ally : nearbyAllies) {
            if(ally.hasFlag()) return ally.getLocation();
        }
        if(swarmLeader != 0) {
            swarmTarget = new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2));
            swarmEnd = rc.getRoundNum() + max(rc.getMapHeight(), rc.getMapWidth()) / 2;
        }
        if(swarmEnd < rc.getRoundNum()) swarmTarget = new MapLocation(-1, -1);
        if(rc.onTheMap(swarmTarget)) return swarmTarget;
        // System.out.println("Swarm retargeted");
        MapLocation[] possibleSenses = rc.senseBroadcastFlagLocations();
        if(possibleSenses.length > 0) {
            swarmTarget = possibleSenses[rng.nextInt(possibleSenses.length)];
            swarmEnd = rc.getRoundNum() + max(rc.getMapHeight(), rc.getMapWidth()) / 2;
            return swarmTarget;
        }
        MapLocation[] possibleCrumbs = rc.senseNearbyCrumbs(-1);
        if(possibleCrumbs.length >= 1) return closest(possibleCrumbs);
        return centre;
    }
    static void attack() throws GameActionException {
        RobotInfo[] possibleEnemies = rc.senseNearbyRobots(4, rc.getTeam().opponent());
        // prioritise flag carriers, tiebreak by lowest hp
        if (possibleEnemies.length >= 1) { // Check if there are enemies in range
            RobotInfo attackTarget = possibleEnemies[0];
            for(RobotInfo enemy : possibleEnemies) {
                if(rc.canAttack(enemy.getLocation())
                   && (enemy.hasFlag() && !attackTarget.hasFlag()
                       || (enemy.hasFlag() == attackTarget.hasFlag() && enemy.health < attackTarget.health))) {
                    attackTarget = enemy;
                }
            }
            if(rc.canAttack(attackTarget.getLocation())) {
                rc.attack(attackTarget.getLocation());
            }
        }
    }
    static void heal() throws GameActionException {
        // Also prioritise flag carriers when healing
        RobotInfo[] nearbyAllyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo healTarget = rc.senseRobot(rc.getID());
        for (RobotInfo ally : nearbyAllyRobots) {
            if(rc.canHeal(ally.getLocation())
               && (ally.hasFlag() && !healTarget.hasFlag()
                   || (ally.hasFlag() == healTarget.hasFlag() && ally.health < healTarget.health))) {
                healTarget = ally;
            }
        }
        if(rc.canHeal(healTarget.getLocation())) {
            rc.heal(healTarget.getLocation());
        }
    }

    static void attackOrHeal() throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots();
        for (RobotInfo robot: nearby) {
            if (robot.hasFlag() && robot.getTeam() == rc.getTeam()) {
                MapLocation loc = robot.getLocation();
                if (rc.canHeal(loc)) {
                    rc.heal(loc);
                    return;
                }
            }
        }
        for (RobotInfo robot: nearby) {
            if (robot.hasFlag() && robot.getTeam() == rc.getTeam().opponent()) {
                MapLocation loc = robot.getLocation();
                if (rc.canAttack(loc)) {
                    rc.attack(loc);
                    return;
                }
            }
        }
        Arrays.sort(nearby, (a, b) -> { return a.getHealth() - b.getHealth(); });
        for (RobotInfo robot: nearby) {
            MapLocation loc = robot.getLocation();
            if (robot.getTeam() == rc.getTeam()) {
                if (rc.canHeal(loc)) {
                    rc.setIndicatorLine(rc.getLocation(), loc, 0, 255, 0);
                    rc.heal(loc);
                    return;
                }
            } else {
                if (rc.canAttack(loc)) {
                    rc.setIndicatorLine(rc.getLocation(), loc, 255, 0, 0);
                    rc.attack(loc);
                    return;
                }
            }
        }
    }

    static void pickupFlag(boolean allowCurrentCell) throws GameActionException {
        if(rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
            for(FlagInfo f : rc.senseNearbyFlags(-1, rc.getTeam().opponent())) {
                if((allowCurrentCell || !f.getLocation().equals(rc.getLocation())) && rc.canPickupFlag(f.getLocation()) && rc.senseNearbyRobots(f.getLocation(), 0, rc.getTeam()).length == 0) {
                    rc.pickupFlag(f.getLocation());
                    readStack();
                    break;
                }
            }
        }
    }

    static void buildTraps() throws GameActionException {
        boolean ok = true;
        for(MapInfo m : rc.senseNearbyMapInfos(-1)) {
            if(m.isWall()) {
                ok = false;
                break;
            }
        }
        for(MapInfo m : rc.senseNearbyMapInfos(2)) {
            if(m.isSpawnZone()) {
                ok = true;
            }
        }
        if(!ok) return;
        if(rc.getCrumbs() >= 200 && rc.getRoundNum() >= 200) {
            TrapType randTrap = new TrapType[]{TrapType.EXPLOSIVE, TrapType.EXPLOSIVE, TrapType.EXPLOSIVE, TrapType.STUN}[rng.nextInt(2)];
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (rc.canBuild(randTrap, rc.getLocation()) && rng.nextInt(max(100 - (30*visibleEnemies.length), 3)) == 0) {
                rc.build(randTrap, rc.getLocation());
            }
        }
    }

    static void shuffle() {
        for (int i = 7; i > 0; --i) {
            int j = rng.nextInt(i + 1);
            Direction temp = directions[i];
            directions[i] = directions[j];
            directions[j] = temp;
        }
    }

    static boolean isCentreSpawn(MapLocation loc) throws GameActionException {
        for (Direction dir: directions) {
            if (!rc.canSenseLocation(loc.add(dir))) {
                return false;
            }
            if (!rc.senseMapInfo(loc.add(dir)).isSpawnZone()) {
                return false;
            }
        }
        return true;
    }

    static void trapSpawn() throws GameActionException {
        if (rc.senseNearbyFlags(-1, rc.getTeam()).length > 0) {
            for (MapInfo info : rc.senseNearbyMapInfos()) {
                if (info.isSpawnZone()) {
                    if (isCentreSpawn(info.getMapLocation())) {
                        if (rc.canBuild(TrapType.STUN, info.getMapLocation())) {
                            rc.build(TrapType.STUN, info.getMapLocation());
                        }
                    } else {
                        if (rc.canBuild(TrapType.EXPLOSIVE, info.getMapLocation())) {
                            rc.build(TrapType.EXPLOSIVE, info.getMapLocation());
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper function used in sorting tiles based on distance.
     * @param target the target tile to which distance is calculated
     * @return a Comparator<MapLocation> which can be passed into the sorting function
     */
    static Comparator<MapLocation> closestComp(MapLocation target) {
        return new Comparator<MapLocation>() {
            @Override
            public int compare(MapLocation o1, MapLocation o2) {
                return target.distanceSquaredTo(o1) - target.distanceSquaredTo(o2);
            }
        };
    }

    /**
     * Helper function used in sorting tiles based on distance to robot's current position.
     * @return a Comparator<MapLocation> which can be passed into the sorting function
     */
    static Comparator<MapLocation> closestComp() {
        return closestComp(rc.getLocation());
    }

    static void printBoard() {
        for (int y = rc.getMapHeight() - 1; y >= 0; --y) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < rc.getMapWidth(); ++x) {
                row.append(board[x][y]);
            }
            System.out.println(row);
        }
    }

    static boolean sameTile(int a, int b) {
        if (a == 0 || b == 0) {
            return true;
        }
        if (a == b) {
            return true;
        }
        if (a == 3 && b == 4) {
            return true;
        }
        if (a == 4 && b == 3) {
            return true;
        }
        return false;
    }

    static void updateCellSymmetry(int x, int y) {
        if (symmetry == Symmetry.HORIZONTAL || symmetry == Symmetry.ALL) {
            board[rc.getMapWidth() - 1 - x][y] = board[x][y];
        }
        if (symmetry == Symmetry.VERTICAL || symmetry == Symmetry.ALL) {
            board[x][rc.getMapHeight() - 1 - y] = board[x][y];
        }
        if (symmetry == Symmetry.ROTATIONAL || symmetry == Symmetry.ALL) {
            board[rc.getMapWidth() - 1 - x][rc.getMapHeight() - 1 - y] = board[x][y];
        }
    }

    static void recordVision() {
        for (MapInfo square: rc.senseNearbyMapInfos()) {
            int x = square.getMapLocation().x;
            int y = square.getMapLocation().y;
            if (board[x][y] == 0) {
                if (square.getSpawnZoneTeam() == team) {
                    board[x][y] = 3;
                } else if (square.getSpawnZoneTeam() == 3 - team) {
                    board[x][y] = 4;
                } else if (square.isWall()) {
                    board[x][y] = 2;
                } else if (!square.isWater() && !square.isPassable()) {
                    // dam
                    board[x][y] = 5;
                } else {
                    board[x][y] = 1;
                }

                if (symmetry == Symmetry.UNKNOWN) {
                    int ver = board[x][rc.getMapHeight() - 1 - y];
                    if (!sameTile(board[x][y], ver)) {
                        vertical = false;
                        if (!horizontal) {
                            symmetry = Symmetry.ROTATIONAL;
                            System.out.println(symmetry.label);
                        } else if (!rotational) {
                            symmetry = Symmetry.HORIZONTAL;
                            System.out.println(symmetry.label);
                        }
                    }
                    int hor = board[rc.getMapWidth() - 1 - x][y];
                    if (!sameTile(board[x][y], hor)) {
                        horizontal = false;
                        if (!vertical) {
                            symmetry = Symmetry.ROTATIONAL;
                            System.out.println(symmetry.label);
                        } else if (!rotational) {
                            symmetry = Symmetry.VERTICAL;
                            System.out.println(symmetry.label);
                        }
                    }
                    int rot = board[rc.getMapWidth() - 1 - x][rc.getMapHeight() - 1 - y];
                    if (!sameTile(board[x][y], rot)) {
                        rotational = false;
                        if (!horizontal) {
                            symmetry = Symmetry.VERTICAL;
                            System.out.println(symmetry.label);
                        } else if (!vertical) {
                            symmetry = Symmetry.HORIZONTAL;
                            System.out.println(symmetry.label);
                        }
                    }
                } else {
                    updateCellSymmetry(x, y);
                }
            }
        }
    }

    public static void run(RobotController _rc) throws GameActionException {
        rc = _rc;
        rng = new Random(rc.getID());
        MapLocation targetCell = new MapLocation(-1, -1);
        centre = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        if (rc.getTeam() == Team.A) {
            team = 1;
        } else {
            team = 2;
        }
        board = new int[rc.getMapWidth()][rc.getMapHeight()];
        while (true) {
            try {
                if (rc.getRoundNum() == 1) {
                    int i = 0;
                    while (rc.readSharedArray(i) > 0) {
                        i++;
                    }
                    rc.writeSharedArray(i, rc.getID() - 9999);
                } else if (rc.getRoundNum() == 2) {
                    for (int i = 0; i < 50; ++i) {
                        int id = rc.readSharedArray(i);
                        ids[i] = id;
                        idx[id] = i;
                        if (id + 9999 == rc.getID()) {
                            selfIdx = i;
                        }
                    }
                    if (selfIdx == 49) {
                        for (int i = 0; i < 50; ++i) {
                            rc.writeSharedArray(i, 0);
                        }
                    }
                }
                if (rc.isSpawned()) {
                    if (selfIdx <= 0) {
                        if (rc.getRoundNum() == 201) {
                            for (int x = 0; x < rc.getMapWidth(); ++x) {
                                for (int y = 0; y < rc.getMapHeight(); ++y) {
                                    if (board[x][y] == 5) {
                                        board[x][y] = 1;
                                    }
                                    if (Clock.getBytecodesLeft() < 4000) {
                                        Clock.yield();
                                    }
                                }
                            }
                            Clock.yield();
                            continue;
                        }
                        // We are the scouting duck! Record all squares we see in the 2d array.
                        recordVision();
                        if (justUpdated) {
                            justUpdated = false;
                            Clock.yield();
                            continue;
                        }
                    }
                }
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    // Arrays.sort(spawnLocs, closestComp(centre));
                    for(int i = spawnLocs.length - 1; i > 0; i--) {
                        int j = rng.nextInt(i);
                        MapLocation tmp = spawnLocs[i];
                        spawnLocs[i] = spawnLocs[j];
                        spawnLocs[j] = tmp;
                    }
                    for (MapLocation loc : spawnLocs) {
                        if (rc.canSpawn(loc)) {
                            rc.spawn(loc);
                            trapSpawn();
                            swarmTarget = new MapLocation(-1, -1);
                            if (rc.getRoundNum() == 1) {
                                shuffle();
                                for (Direction dir : directions) {
                                    if (rc.canMove(dir)) {
                                        if (!rc.senseMapInfo(rc.getLocation().add(dir)).isSpawnZone()) {
                                            rc.move(dir);
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                } else if (rc.getRoundNum() <= 150) {
                    nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
                    trapSpawn();
                    MapLocation nextLoc;
                    MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
                    if(crumbs.length > 0) {
                        // sort crumbs by distance
                        Arrays.sort(crumbs, closestComp());
                        nextLoc = crumbs[0];
                    } else {
                        if(movesLeft > 0 && !rc.getLocation().equals(target)) {
                            movesLeft--;
                        } else {
                            target = new MapLocation(Math.max(0, Math.min(rc.getMapWidth() - 1, rc.getLocation().x + rng.nextInt(21) - 10)),
                                    Math.max(0, Math.min(rc.getMapHeight() - 1, rc.getLocation().y + rng.nextInt(21) - 10)));
                            movesLeft = 7;
                        }
                        nextLoc = target;
                    }
                    moveBetter(nextLoc);
                } else {
                    nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
                    pickupFlag(false);
                    targetCell = findTarget();
                    if(rc.senseNearbyFlags(0).length == 0) attackOrHeal();
                    trapSpawn();
                    // Determine whether to move or not
                    int nearbyHP = rc.getHealth();
                    for (RobotInfo ally : nearbyAllies) {
                        nearbyHP+=ally.health;
                    }
                    nearbyHP /= (nearbyAllies.length+1);
                    int threshold = min(nearbyAllies.length*75, 751);
                    // Movement
                    {
                        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                        if (enemies.length * 3 > nearbyAllies.length * 2
                            && !rc.hasFlag()
                            && rc.senseNearbyFlags(9, rc.getTeam().opponent()).length == 0
                            && rc.senseNearbyFlags(4, rc.getTeam()).length == 0) {
                            int minTargeted = 0;
                            Direction[] choices = new Direction[8];
                            MapLocation loc = rc.getLocation();
                            for (RobotInfo enemy : enemies) {
                                if (enemy.getLocation().isWithinDistanceSquared(loc, 4)) {
                                    minTargeted++;
                                }
                            }
                            int n = 0;
                            shuffle();
                            for (Direction dir : directions) {
                                if (rc.canMove(dir)) {
                                    int count = 0;
                                    for (RobotInfo enemy : enemies) {
                                        if (enemy.getLocation().isWithinDistanceSquared(loc.add(dir), 4)) {
                                            count++;
                                        }
                                    }
                                    if (count < minTargeted) {
                                        minTargeted = count;
                                        n = 0;
                                    }
                                    if (count == minTargeted) {
                                        choices[n++] = dir;
                                    }
                                }
                            }
                            if (n > 0) {
                                MapLocation finalTargetCell = targetCell;
                                Arrays.sort(choices, 0, n, (a, b) -> {
                                    return rc.getLocation().add(a).distanceSquaredTo(finalTargetCell) - rc.getLocation().add(b).distanceSquaredTo(finalTargetCell);
                                });
                                rc.move(choices[0]);
                            } else {
                                shuffle();
                                int minAdj = 0;
                                for (RobotInfo friend : rc.senseNearbyRobots(-1, rc.getTeam())) {
                                    if (friend.getLocation().isWithinDistanceSquared(rc.getLocation(), 2)) {
                                        minAdj++;
                                    }
                                }
                                Direction choice = Direction.NORTH;
                                boolean overriden = false;
                                for (Direction dir : directions) {
                                    if (rc.canMove(dir)) {
                                        int adjCount = 0;
                                        for (RobotInfo friend : rc.senseNearbyRobots(-1, rc.getTeam())) {
                                            if (friend.getLocation().isWithinDistanceSquared(rc.getLocation().add(dir), 2)) {
                                                adjCount++;
                                            }
                                        }
                                        if (adjCount < minAdj) {
                                            choice = dir;
                                            minAdj = adjCount;
                                            overriden = true;
                                        }
                                    }
                                }
                                if (overriden) {
                                    rc.move(choice);
                                }
                            }
                        } else if (nearbyHP >= threshold) {
                            moveBetter(targetCell);
                            rc.setIndicatorString("Nope, not kiting");
                        }
                    }
                    pickupFlag(true);
                    attackOrHeal();
                    buildTraps();
                    // Attempt to buy global upgrades
                    if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
                        rc.buyGlobal(GlobalUpgrade.ACTION);
                    }
                    if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                        rc.buyGlobal(GlobalUpgrade.HEALING);
                    }
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                if (rc.onTheMap(targetCell) && rc.isSpawned()) {
                    rc.setIndicatorLine(rc.getLocation(), targetCell, 0, 255, 0);
                }
                rc.setIndicatorString(String.valueOf("TurnDir: " + String.valueOf(turnDir) + " StackSize: " + String.valueOf(stackSize) + " Cooldowns: " + rc.getMovementCooldownTurns()) + " " + String.valueOf(rc.getActionCooldownTurns()));
                Clock.yield();
            }
        }
    }
}
