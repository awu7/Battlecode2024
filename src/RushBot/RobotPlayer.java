package RushBot;

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
            Direction.SOUTHEAST,
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST,
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
    };
    static String indicatorString = "";
    static <T> void debug(T x) {
        indicatorString += String.valueOf(x) + "; ";
    }
    static MapLocation centre;
    static MapLocation[] spawnCentres = new MapLocation[3];
    static int[] builders = new int[10000];
    static int movesLeft = 0;
    static MapLocation target;
    static int team = 0;
    static int round;

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
    // 1 = empty passable tile
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

    /**
     * Helper function to check if a friend can pick up a flag dropped in a direction.
     * @param dir the direction to drop the flag.
     * @return
     */
    static boolean canFriendPickup(Direction dir) throws GameActionException {
        MapLocation loc = rc.getLocation().add(dir);
        RobotInfo robot = rc.senseRobotAtLocation(loc);
        if (robot != null && robot.getTeam() == rc.getTeam()) {
            return true;
        }
        robot = rc.senseRobotAtLocation(loc.add(dir));
        if (robot != null && robot.getTeam() == rc.getTeam()) {
            return true;
        }
        robot = rc.senseRobotAtLocation(loc.add(dir.rotateRight()));
        if (robot != null && robot.getTeam() == rc.getTeam()) {
            return true;
        }
        robot = rc.senseRobotAtLocation(loc.add(dir.rotateLeft()));
        if (robot != null && robot.getTeam() == rc.getTeam()) {
            return true;
        }
        return false;
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
        if(stackSize != 0 && (!rc.getLocation().directionTo(pos).equals(stack[0]) || rng.nextInt(16) == 0)) {
            debug("Stack reset");
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
                    debug("Passing flag");
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
        if (rc.canFill(nextLoc) && !hasFlag) {
            rc.fill(nextLoc);
        }
        if(rc.canMove(dir)) {
            rc.move(dir);
        }
        nextLoc = rc.getLocation().add(dir);
        if(rc.onTheMap(nextLoc)) {
            nextLocRobot = rc.senseRobotAtLocation(nextLoc);
            if(rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.getTeam() == rc.getTeam()) {
                rc.dropFlag(nextLoc);
                writeStack();
            }
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
        int swarmLeader = rc.readSharedArray(0);
        if(swarmLeader == rc.getID()) {
            rc.writeSharedArray(0, 0);
        }
        if(rc.hasFlag()) return closest(rc.getAllySpawnLocations());
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
    static void healFlagBearer() throws GameActionException {
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
    }
    static void heal() throws GameActionException {
        // Also prioritise flag carriers when healing
        RobotInfo[] nearbyAllyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo healTarget = rc.senseRobot(rc.getID());
        for (RobotInfo ally : nearbyAllyRobots) {
            if(rc.canHeal(ally.getLocation())
                    && ally.health < healTarget.health) {
                healTarget = ally;
            }
        }
        if(rc.canHeal(healTarget.getLocation())) {
            rc.heal(healTarget.getLocation());
        }
    }

    static void attackOrHeal() throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots();
        healFlagBearer();
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
            TrapType randTrap = new TrapType[]{TrapType.EXPLOSIVE, TrapType.STUN, TrapType.STUN, TrapType.STUN}[rng.nextInt(2)];
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
        if (rc.canBuild(TrapType.STUN, rc.getLocation())) {
            rc.build(TrapType.STUN, rc.getLocation());
        }
        for (int i=0; i<4; i++){
            if (rc.canBuild(TrapType.WATER, rc.getLocation().add(directions[i]))) {
                rc.build(TrapType.WATER, rc.getLocation().add(directions[i]));
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

    /**
     * Updates the contents of a cell using knowledge of symmetry.
     * @param x the x coordinate
     * @param y the y coordinate
     */
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

    /**
     * Records the squares in the vision radius seen by the current bot.
     */
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

    public static void broadcastVision(int arrayIdx) throws GameActionException {
        int x = rc.getLocation().x, y = rc.getLocation().y;
        int hash1 = x * rc.getMapHeight() + y, hash2 = 0;
        rc.writeSharedArray(arrayIdx, hash1);
        MapLocation loc;
        if(rc.onTheMap(loc = rc.getLocation().translate(-2, 3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1;
        if(rc.onTheMap(loc = rc.getLocation().translate(-1, 3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 1;
        if(rc.onTheMap(loc = rc.getLocation().translate(0, 3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 2;
        if(rc.onTheMap(loc = rc.getLocation().translate(1, 3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 3;
        if(rc.onTheMap(loc = rc.getLocation().translate(2, 3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 4;
        if(rc.onTheMap(loc = rc.getLocation().translate(3, 2)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 5;
        if(rc.onTheMap(loc = rc.getLocation().translate(3, 1)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 6;
        if(rc.onTheMap(loc = rc.getLocation().translate(3, 0)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 7;
        if(rc.onTheMap(loc = rc.getLocation().translate(3, -1)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 8;
        if(rc.onTheMap(loc = rc.getLocation().translate(3, -2)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 9;
        if(rc.onTheMap(loc = rc.getLocation().translate(2, -3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 10;
        if(rc.onTheMap(loc = rc.getLocation().translate(1, -3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 11;
        if(rc.onTheMap(loc = rc.getLocation().translate(0, -3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 12;
        if(rc.onTheMap(loc = rc.getLocation().translate(-1, -3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 13;
        if(rc.onTheMap(loc = rc.getLocation().translate(-2, -3)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 14;
        if(rc.onTheMap(loc = rc.getLocation().translate(-3, -2)) && rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 15;
        if(rc.onTheMap(loc = rc.getLocation().translate(-3, -1)) && rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 12;
        if(rc.onTheMap(loc = rc.getLocation().translate(-3, 0)) && rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 13;
        if(rc.onTheMap(loc = rc.getLocation().translate(-3, 1)) && rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 14;
        if(rc.onTheMap(loc = rc.getLocation().translate(-3, 2)) && rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 15;
        rc.writeSharedArray(arrayIdx, hash1);
        rc.writeSharedArray(arrayIdx | 1, hash2);
    }

    public static void decodeBroadcast(int arrIdx) throws GameActionException {
        int hash1 = rc.readSharedArray(arrIdx), hash2 = rc.readSharedArray(arrIdx | 1);
        int locHash = hash1 & ((1 << 12) - 1);
        int x = locHash / rc.getMapHeight(), y = locHash % rc.getMapHeight();
        if(rc.onTheMap(new MapLocation(x - 2, y + 3))) board[x - 2][y + 3] = (hash2 >>> 0) & 1;
        if(rc.onTheMap(new MapLocation(x - 1, y + 3))) board[x - 1][y + 3] = (hash2 >>> 1) & 1;
        if(rc.onTheMap(new MapLocation(x + 0, y + 3))) board[x + 0][y + 3] = (hash2 >>> 2) & 1;
        if(rc.onTheMap(new MapLocation(x + 1, y + 3))) board[x + 1][y + 3] = (hash2 >>> 3) & 1;
        if(rc.onTheMap(new MapLocation(x + 2, y + 3))) board[x + 2][y + 3] = (hash2 >>> 4) & 1;
        if(rc.onTheMap(new MapLocation(x + 3, y + 2))) board[x + 3][y + 2] = (hash2 >>> 5) & 1;
        if(rc.onTheMap(new MapLocation(x + 3, y + 1))) board[x + 3][y + 1] = (hash2 >>> 6) & 1;
        if(rc.onTheMap(new MapLocation(x + 3, y + 0))) board[x + 3][y + 0] = (hash2 >>> 7) & 1;
        if(rc.onTheMap(new MapLocation(x + 3, y - 1))) board[x + 3][y - 1] = (hash2 >>> 8) & 1;
        if(rc.onTheMap(new MapLocation(x + 3, y - 2))) board[x + 3][y - 2] = (hash2 >>> 9) & 1;
        if(rc.onTheMap(new MapLocation(x + 2, y - 3))) board[x + 2][y - 3] = (hash2 >>> 10) & 1;
        if(rc.onTheMap(new MapLocation(x + 1, y - 3))) board[x + 1][y - 3] = (hash2 >>> 11) & 1;
        if(rc.onTheMap(new MapLocation(x + 0, y - 3))) board[x + 0][y - 3] = (hash2 >>> 12) & 1;
        if(rc.onTheMap(new MapLocation(x - 1, y - 3))) board[x - 1][y - 3] = (hash2 >>> 13) & 1;
        if(rc.onTheMap(new MapLocation(x - 2, y - 3))) board[x - 2][y - 3] = (hash2 >>> 14) & 1;
        if(rc.onTheMap(new MapLocation(x - 3, y - 2))) board[x - 3][y - 2] = (hash2 >>> 15) & 1;
        if(rc.onTheMap(new MapLocation(x - 3, y - 1))) board[x - 3][y - 1] = (hash1 >>> 12) & 1;
        if(rc.onTheMap(new MapLocation(x - 3, y + 0))) board[x - 3][y + 0] = (hash1 >>> 13) & 1;
        if(rc.onTheMap(new MapLocation(x - 3, y + 1))) board[x - 3][y + 1] = (hash1 >>> 14) & 1;
        if(rc.onTheMap(new MapLocation(x - 3, y + 2))) board[x - 3][y + 2] = (hash1 >>> 15) & 1;
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
        System.out.println(Clock.getBytecodeNum());
        while (true) {
            try {
                round = rc.getRoundNum();
                if (round == 1) {
                    int i = 0;
                    while (rc.readSharedArray(i) > 0) {
                        i++;
                    }
                    rc.writeSharedArray(i, rc.getID() - 9999);
                } else if (round == 2) {
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
                    int f = 0;
                    for (MapLocation m : rc.getAllySpawnLocations()){
                        int adjCount = 0;
                        for(MapLocation m2 : rc.getAllySpawnLocations()) {
                            if(Math.abs(m.x - m2.x) <= 1 && Math.abs(m.y - m2.y) <= 1) adjCount++;
                        }
                        if (adjCount == 9){
                            spawnCentres[f] = m;
                            f++;
                        }
                    }
                    for (int i=40; i<43; i++){
                        builders[ids[i]] = i;
                    }
                }
                if (rc.isSpawned()) {
                    debug(idx[rc.getID()-9999]);
                    if (selfIdx <= 0) {
                        if (round >= 3 && round <= 150) {
                            System.out.println(Clock.getBytecodeNum());
                            for (int i = 4; i < 64; i += 2) {
                                decodeBroadcast(i);
                            }
                            System.out.println(Clock.getBytecodeNum());
                        }
                        if (round == 201) {
                            // for (int x = 0; x < rc.getMapWidth(); ++x) {
                            //     for (int y = 0; y < rc.getMapHeight(); ++y) {
                            //         if (board[x][y] == 5) {
                            //             board[x][y] = 1;
                            //         }
                            //         if (Clock.getBytecodesLeft() < 4000) {
                            //             Clock.yield();
                            //         }
                            //     }
                            // }
                            // Clock.yield();
                            // continue;
                            // printBoard();
                        }
                        // We are the scouting duck! Record all squares we see in the 2d array.
                        //     recordVision();
                        // if (justUpdated) {
                        //     justUpdated = false;
                        //     Clock.yield();
                        //     continue;
                        // }
                    } else if (round >= 3 && round <= 150 && selfIdx >= 2 && selfIdx <= 31) {
                        broadcastVision(selfIdx << 1);
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
                            swarmTarget = new MapLocation(-1, -1);
                            if (round == 1) {
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
                } else if (round <= 150){
                    if (builders[rc.getID()-9999] != 0){
                        if (!rc.getLocation().equals(spawnCentres[builders[rc.getID()-9999]-40])){
                            moveBetter(spawnCentres[builders[rc.getID()-9999]-40]);
                            rc.setIndicatorLine(rc.getLocation(), spawnCentres[builders[rc.getID()-9999]-40], 0, 0, 255);
                        }
                        if (rc.getLocation().equals(spawnCentres[builders[rc.getID()-9999]-40])) trapSpawn();
                        continue;
                    }
                    nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
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
                }  else if (!rc.hasFlag() && rc.senseNearbyFlags(0, rc.getTeam().opponent()).length >= 1 && !rc.canPickupFlag(rc.getLocation())) {
                    // wait, we need to pick up a flag dropped by a teammate
                } else {
                    if (builders[rc.getID()-9999] != 0){
                        if (!rc.getLocation().equals(spawnCentres[builders[rc.getID()-9999]-40])){
                            moveBetter(spawnCentres[builders[rc.getID()-9999]-40]);
                            rc.setIndicatorLine(rc.getLocation(), spawnCentres[builders[rc.getID()-9999]-40], 0, 0, 255);
                        }
                        if (rc.getLocation().equals(spawnCentres[builders[rc.getID()-9999]-40])) trapSpawn();
                        continue;
                    }
                    nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
                    pickupFlag(true);
                    targetCell = findTarget();
                    if(rc.senseNearbyFlags(0).length == 0) {
                        healFlagBearer();
                        attack();
                    }
                    // Determine whether to move or not
                    int nearbyHP = rc.getHealth();
                    for (RobotInfo ally : nearbyAllies) {
                        nearbyHP += ally.health;
                    }
                    int threshold = min(nearbyAllies.length * 75, 751) * (nearbyAllies.length + 1);
                    int enemyHP = 0;
                    RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                    for(RobotInfo enemy : enemies) {
                        enemyHP += enemy.health;
                    }
                    // Movement
                    {
                        if (enemyHP * 5 > nearbyHP * 2
                                && !rc.hasFlag()
                                && rc.senseNearbyFlags(9, rc.getTeam().opponent()).length == 0
                                && rc.senseNearbyFlags(4, rc.getTeam()).length == 0) {
                            // Begin Kiting
                            int currTargeted = 0;
                            Direction[] choices = new Direction[8];
                            MapLocation rcLocation = rc.getLocation();
                            boolean returnToCombat = rc.isActionReady(); // Only return if currently safe
                            for (RobotInfo enemy : enemies) {
                                if (enemy.getLocation().isWithinDistanceSquared(rcLocation, 4)) {
                                    currTargeted++;
                                    returnToCombat = false;
                                }
                            }
                            int minTargeted = currTargeted;
                            if (returnToCombat) {
                                minTargeted = 1000; // Reset min
                            }
                            int n = 0; // size of choices;
                            shuffle();
                            for (Direction dir : directions) {
                                if (rc.canMove(dir)) {
                                    int count = 0;
                                    for (RobotInfo enemy : enemies) {
                                        if (enemy.getLocation().isWithinDistanceSquared(rcLocation.add(dir), 4)) {
                                            count++;
                                        }
                                    }
                                    if (!(returnToCombat && count == 0)) {
                                        if (count < minTargeted) {
                                            minTargeted = count;
                                            n = 0;
                                        }
                                        if (count == minTargeted) {
                                            choices[n++] = dir;
                                        }
                                    }
                                }
                            }
                            if (n > 0) {
                                // Choose the best choice
                                // Choices are either the safest, or safest where we can attack
                                MapLocation finalTargetCell = targetCell;
                                Arrays.sort(choices, 0, n, (a, b) -> {
                                    return rc.getLocation().add(a).distanceSquaredTo(finalTargetCell) - rc.getLocation().add(b).distanceSquaredTo(finalTargetCell);
                                });
                                rc.move(choices[0]);
                            } else {
                                // There are no choices
                                shuffle();
                                int minAdj = rc.senseNearbyRobots(2, rc.getTeam()).length;
                                Direction choice = Direction.NORTH;
                                boolean overridden = false;
                                for (Direction dir : directions) {
                                    if (rc.canMove(dir)) {
                                        int adjCount = rc.senseNearbyRobots(-1, rc.getTeam()).length;
                                        if (adjCount < minAdj) {
                                            choice = dir;
                                            minAdj = adjCount;
                                            overridden = true;
                                        }
                                    }
                                }
                                if (overridden) {
                                    rc.move(choice);
                                } else if (returnToCombat) {
                                    // Attempt to move towards enemies
                                    moveBetter(enemies[0].getLocation());
                                }
                            }
                        } else if (nearbyHP >= threshold || rc.senseNearbyFlags(13, rc.getTeam().opponent()).length == 0) {
                            moveBetter(targetCell);
                            debug(targetCell);
                            debug("Nope, not kiting");
                        }
                    }
                    pickupFlag(true);
                    healFlagBearer();
                    attack();
                    heal();
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
                debug("TD: " + String.valueOf(turnDir));
                debug("SS: " + String.valueOf(stackSize));
                rc.setIndicatorString(indicatorString);
                indicatorString = "";
                Clock.yield();
            }
        }
    }
}