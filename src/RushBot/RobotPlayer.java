package RushBot;

import battlecode.common.*;

import java.lang.System;
import java.util.*;
import java.util.function.ToIntBiFunction;

class ActiveStun {
    public MapLocation location = null;
    public int roundsLeft;
    public ActiveStun(MapLocation locIn) {
        location = locIn;
        roundsLeft = 4;
    }
    /**
     * Returns false if this stun is ineffective after updating,
     * otherwise true
     */
    public boolean updateRound() {
        roundsLeft--;
        if (roundsLeft <= 0) {
            return false;
        }
        return true;
    }
}

public strictfp class RobotPlayer {
    static RobotController rc;
    static Random rng;
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST,
    };
    static Direction[] shuffledDirections = {
            Direction.SOUTHEAST,
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST,
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
    };
    static final String[] dirStrs = {};
    static final Direction[] trapDirs = {
            Direction.SOUTHEAST,
            Direction.NORTHEAST,
            Direction.WEST,
    };

    static MapLocation centre;
    static MapLocation[] spawnCentres = new MapLocation[3];
    static int isBuilder = 0;
    static int movesLeft = 0;
    static MapLocation target;
    static int team = 0;
    static int round;
    /**
     * Array of all previous stuns
     */
    static List<MapLocation> prevStuns = new ArrayList<MapLocation>();
    static List<ActiveStun> activeStuns = new ArrayList<ActiveStun>();
    static RobotInfo[] sittingDucks;
    static void updateStuns() throws GameActionException {
        List<MapLocation> currStuns = new ArrayList<MapLocation>();
        MapInfo[] mapInfos = rc.senseNearbyMapInfos(-1);
        for (MapInfo mi : mapInfos) {
            if (mi.getTrapType() == TrapType.STUN) {
                currStuns.add(mi.getMapLocation());
            }
        }
        for (MapLocation ml : prevStuns) {
            if (ml.distanceSquaredTo(rc.getLocation()) < 20) {
                boolean triggered = true;
                for (MapLocation ml2 : currStuns) {
                    if (ml2.equals(ml)) {
                        triggered = false;
                        break;
                    }
                }
                if (triggered) {
                    activeStuns.add(new ActiveStun(ml));
                }
            }
        }
        prevStuns = currStuns;
        debug("currStuns: " + currStuns.size());
        List<ActiveStun> newActiveStuns = new ArrayList<ActiveStun>();
        for (ActiveStun stun : activeStuns) {
            if (stun.updateRound()) {
                newActiveStuns.add(stun);
            }
        }
        activeStuns = newActiveStuns;
    }

    static String indicatorString = "";

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
     * [2, 31] = scouters, broadcasts everything seen in allocated spot in array.
     * [45, 49] = BFS bots, receives information from other bots and runs BFS in idle turns.
     */
    static int selfIdx = -1;

    static Direction[] stack = new Direction[10];
    static int stackSize = 0;
    static int turnDir = 0;
    static RobotInfo[] nearbyAllies;

    /**
     * 2d array storing the board
     * 0 = undiscovered
     * 1 = empty passable tile
     * 2 = wall
     * 3 = our spawn zone
     * 4 = opponent's spawn zone
     */
    static int[][] board;
    /**
     * 2d array storing a simplified version of the board with weights, used for BFS.
     */
    static boolean vertical = true, horizontal = true, rotational = true;
    public enum Symmetry {
        UNKNOWN("Unknown"),
        VERTICAL("Vertical"),
        HORIZONTAL("Horizontal"),
        ROTATIONAL("Rotational");

        public final String label;

        Symmetry(String label) {
            this.label = label;
        }
    }
    static Symmetry symmetry = Symmetry.UNKNOWN;
    static ToIntBiFunction<Integer, Integer> getOpp = (x, y) -> { return 1; };
    static int width, height;
    static int widthMinus1, widthMinus2, widthMinus3;
    static int heightMinus1,heightMinus2, heightMinus3;

    static StringBuilder q;
    static int[][] bfs;
    static int[][] optimal;
    static boolean bfsDone = false;
    static int bfsIdx = -2;
    static int internalIdx = 0;
    
    static int[][] centreBfs, spawnBfs, flag0Bfs, flag1Bfs, flag2Bfs;
    public enum BfsTarget {
        CENTRE,
        SPAWN,
        FLAG0,
        FLAG1,
        FLAG2
    }

    /**
     * Helper function to append to the bot's indicator string.
     * @param x the value to append.
     * @param <T> the type of the value.
     */
    static <T> void debug(T x) {
        indicatorString += String.valueOf(x) + "; ";
    }

    public static MapLocation unhashChar(char c) {
        return new MapLocation(c / height, c % height);
    }

    public static char hashLoc(MapLocation loc) {
        return (char) (loc.x * height + loc.y);
    }

    /**
     * Helper function to calculate the minimum distance to the edge of
     * the map
     * @param loc Location to find the smallest distance to the edge from
     * @return The smallest distance to the edge of the map
     */
    static int distFromEdge(MapLocation loc) {
        return StrictMath.min(StrictMath.min(loc.x, rc.getMapWidth() - loc.x), StrictMath.min(loc.y, rc.getMapHeight() - loc.y));
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
        if(locs.length == 0) return new MapLocation(-1, -1);
        int mn = 1000000000;
        MapLocation res = locs[0];
        for (int i = 0; i < locs.length; i++) {
            if(!rc.onTheMap(locs[i])) continue;
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
                debug("Passed flag in moveBetter()");
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
            if(!possibleFlags[i].isPickedUp()) flagLocs[i] = possibleFlags[i].getLocation();
            else flagLocs[i] = new MapLocation(-1, -1);
        }
        MapLocation closestFlag = closest(flagLocs);
        if(rc.onTheMap(closestFlag)) return closestFlag;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapLocation[] enemyLocs = new MapLocation[enemies.length];
        for(int i = 0; i < enemies.length; i++) {
            enemyLocs[i] = enemies[i].getLocation();
        }
        if(enemies.length >= 1) {
            return closest(enemyLocs);
        }
        if(swarmLeader != 0) {
            swarmTarget = new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2));
            swarmEnd = rc.getRoundNum() + StrictMath.max(height, width) / 2;
        }
        if(swarmEnd < rc.getRoundNum()) swarmTarget = new MapLocation(-1, -1);
        if(rc.onTheMap(swarmTarget)) return swarmTarget;
        // System.out.println("Swarm retargeted");
        MapLocation[] possibleSenses = rc.senseBroadcastFlagLocations();
        if(possibleSenses.length > 0) {
            swarmTarget = possibleSenses[rng.nextInt(possibleSenses.length)];
            swarmEnd = rc.getRoundNum() + StrictMath.max(height, width) / 2;
            return swarmTarget;
        }
        MapLocation[] possibleCrumbs = rc.senseNearbyCrumbs(-1);
        if(possibleCrumbs.length >= 1) return closest(possibleCrumbs);
        return centre;
    }
    static void attack() throws GameActionException {
        RobotInfo[] possibleEnemies = rc.senseNearbyRobots(4, rc.getTeam().opponent());
        // prioritise flag carriers, then sitting ducks, tiebreak by lowest hp
        // todone: implement prioritise sitting ducks
        // Not sure if it was done correctly but should be done
        if (possibleEnemies.length >= 1) { // Check if there are enemies in range
            RobotInfo attackTarget = possibleEnemies[0];
            int currPriority = 0;
            for(RobotInfo enemy : possibleEnemies) {
                boolean flagPriority = enemy.hasFlag;
                boolean stunPriority = false;
                /*
                // nullptr exception due to sittingDucks being uninitialised
                // because it's calculated after first call of attack()
                for (RobotInfo stunned : sittingDucks) {
                    if (stunned == null) {
                        continue;
                    }
                    if (stunned.ID == enemy.ID) {
                        stunPriority = true;
                        break;
                    }
                }*/
                int totalPriority = (flagPriority?2:0)+(stunPriority?1:0);
                // If target is of higher priority than current, retarget
                // If target is of same priority as current, and has less health, retarget
                if (totalPriority == 3) {
                    totalPriority = 2;
                } else if (totalPriority == 2) {
                    totalPriority = 3;
                }
                if(rc.canAttack(enemy.getLocation())
                        && (totalPriority > currPriority
                        || (totalPriority == currPriority && enemy.health < attackTarget.health))) {
                    attackTarget = enemy;
                    currPriority = totalPriority;
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

    public static void pickupFlagUtil(FlagInfo flag) throws GameActionException {
        MapLocation flagLoc = flag.getLocation();
        if (!rc.canPickupFlag(flagLoc)) {
            return;
        }
        if (flagLoc.equals(rc.getLocation())) {
            rc.pickupFlag(flagLoc);
            return;
        }
        int i = spawnBfs != null ? spawnBfs[flagLoc.x][flagLoc.y] : 0;
        if (i > 0) {
            Direction dir = directions[i - 1];
            for (Direction choice: new Direction[]{dir, dir.rotateLeft(), dir.rotateRight()}) {
                MapLocation next = flagLoc.add(choice);
                if (next.equals(rc.getLocation())) {
                    rc.pickupFlag(flagLoc);
                    readStack();
                    return;
                }
                RobotInfo friend = rc.senseRobotAtLocation(next);
                if (friend != null && friend.getTeam() == rc.getTeam()) {
                    debug("Letting friend pickup");
                    return;
                }
            }
        }
        rc.pickupFlag(flagLoc);
        readStack();
    }

    static void pickupFlag(boolean allowCurrentCell) throws GameActionException {
        if(rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
            for(FlagInfo f : rc.senseNearbyFlags(-1, rc.getTeam().opponent())) {
                pickupFlagUtil(f);
            }
        }
    }

    static void buildTraps() throws GameActionException {
        boolean ok = true;
        MapInfo[] mapInfos = rc.senseNearbyMapInfos(3);
        for(MapInfo m : mapInfos) {
            if(m.isWall()) {
                ok = false;
                break;
            }
        }
        for(MapInfo m : rc.senseNearbyMapInfos(2)) {
            if(m.isSpawnZone() && m.getSpawnZoneTeam() != team) {
                ok = true;
            }
        }
        if(!ok) return;
        if(rc.getCrumbs() >= 250 && rc.getRoundNum() >= 200) {
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            // Calculate number of nearby traps
            int nearbyTraps = 0;
            for (MapInfo mi : mapInfos) {
                if (mi.getTrapType() != TrapType.NONE) {
                    ++nearbyTraps;
                }
            }
            for(Direction d : Direction.values()) {
                boolean adjTrap = false;
                TrapType chosenTrap = rng.nextInt(2) == 0 ? TrapType.STUN : TrapType.EXPLOSIVE;
                for(MapInfo m : rc.senseNearbyMapInfos(rc.getLocation().add(d), 4)) {
                    if(m.getTrapType() != TrapType.NONE) adjTrap = true;
                }
                int chanceReciprocal = 3 * StrictMath.max(StrictMath.min(StrictMath.max(10 - (3 * visibleEnemies.length), 2), 2+nearbyTraps*2), 21 - 3 * StrictMath.min(distFromEdge(rc.getLocation()), 7));
                if((!adjTrap || rc.getCrumbs() > 5000) && rc.canBuild(chosenTrap, rc.getLocation().add(d)) && rng.nextInt(chanceReciprocal) == 0) {
                    rc.build(chosenTrap, rc.getLocation().add(d));
                }
            }
        }
    }

    static void shuffle() {
        for (int i = 7; i > 0; --i) {
            int j = rng.nextInt(i + 1);
            Direction temp = shuffledDirections[i];
            shuffledDirections[i] = shuffledDirections[j];
            shuffledDirections[j] = temp;
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
        for (int i=0; i<3; i++){
            if (rc.canBuild(TrapType.STUN, rc.getLocation().add(trapDirs[i]))) {
                rc.build(TrapType.STUN, rc.getLocation().add(trapDirs[i]));
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
        for (int y = height - 1; y >= 0; --y) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < width; ++x) {
//                if (optimal[x][y] < 10) row.append(' ');
//                if (optimal[x][y] < 0) row.append(' ');
//                else row.append(optimal[x][y]);
                row.append(" ");
                if (spawnBfs[x][y] == 0) row.append(" ");
                else row.append(dirStrs[spawnBfs[x][y] - 1]);
            }
            System.out.println(row);
        }
    }

    static boolean sameTile(int a, int b) {
        if (a == 0 || b == 0) {
            return true;
        }
        if (a == b && !(a == 3 || a == 4)) {
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
        if (symmetry == Symmetry.HORIZONTAL) {
            board[width - 1 - x][y] = board[x][y];
        }
        if (symmetry == Symmetry.VERTICAL) {
            board[x][height - 1 - y] = board[x][y];
        }
        if (symmetry == Symmetry.ROTATIONAL) {
            board[width - 1 - x][height - 1 - y] = board[x][y];
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
                    int ver = board[x][height - 1 - y];
                    if (!sameTile(board[x][y], ver)) {
                        vertical = false;
                        if (!horizontal) {
                            symmetry = Symmetry.ROTATIONAL;
                        } else if (!rotational) {
                            symmetry = Symmetry.HORIZONTAL;
                        }
                    }
                    int hor = board[width - 1 - x][y];
                    if (!sameTile(board[x][y], hor)) {
                        horizontal = false;
                        if (!vertical) {
                            symmetry = Symmetry.ROTATIONAL;
                        } else if (!rotational) {
                            symmetry = Symmetry.VERTICAL;
                        }
                    }
                    int rot = board[width - 1 - x][height - 1 - y];
                    if (!sameTile(board[x][y], rot)) {
                        rotational = false;
                        if (!horizontal) {
                            symmetry = Symmetry.VERTICAL;
                        } else if (!vertical) {
                            symmetry = Symmetry.HORIZONTAL;
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
        int hash1 = x * height + y, hash2 = 0;
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
        int x = locHash / height, y = locHash % height;
        if (symmetry == Symmetry.VERTICAL) {
            if (x >= 2 && y < heightMinus3) board[x - 2][heightMinus1 - (y + 3)] = board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < heightMinus3) board[x - 1][heightMinus1 - (y + 3)] = board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < heightMinus3) board[x][heightMinus1 - (y + 3)] = board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < widthMinus1 && y < heightMinus3) board[x + 1][heightMinus1 - (y + 3)] = board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < widthMinus2 && y < heightMinus3) board[x + 2][heightMinus1 - (y + 3)] = board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < widthMinus3 && y < heightMinus2) board[x + 3][heightMinus1 - (y + 2)] = board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < widthMinus3 && y < heightMinus1) board[x + 3][heightMinus1 - (y + 1)] = board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < widthMinus3) board[x + 3][heightMinus1 - (y)] = board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < widthMinus3 && y >= 1) board[x + 3][heightMinus1 - (y - 1)] = board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < widthMinus3 && y >= 2) board[x + 3][heightMinus1 - (y - 2)] = board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < widthMinus2 && y >= 3) board[x + 2][heightMinus1 - (y - 3)] = board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < widthMinus1 && y >= 3) board[x + 1][heightMinus1 - (y - 3)] = board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) board[x][heightMinus1 - (y - 3)] = board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) board[x - 1][heightMinus1 - (y - 3)] = board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) board[x - 2][heightMinus1 - (y - 3)] = board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) board[x - 3][heightMinus1 - (y - 2)] = board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) board[x - 3][heightMinus1 - (y - 1)] = board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) board[x - 3][heightMinus1 - (y)] = board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < heightMinus1) board[x - 3][heightMinus1 - (y + 1)] = board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < heightMinus2) board[x - 3][heightMinus1 - (y + 2)] = board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        } else if (symmetry == Symmetry.HORIZONTAL) {
            if (x >= 2 && y < heightMinus3) board[widthMinus1 - (x - 2)][y + 3] = board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < heightMinus3) board[widthMinus1 - (x - 1)][y + 3] = board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < heightMinus3) board[widthMinus1 - (x)][y + 3] = board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < widthMinus1 && y < heightMinus3) board[widthMinus1 - (x + 1)][y + 3] = board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < widthMinus2 && y < heightMinus3) board[widthMinus1 - (x + 2)][y + 3] = board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < widthMinus3 && y < heightMinus2) board[widthMinus1 - (x + 3)][y + 2] = board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < widthMinus3 && y < heightMinus1) board[widthMinus1 - (x + 3)][y + 1] = board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < widthMinus3) board[widthMinus1 - (x + 3)][y] = board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < widthMinus3 && y >= 1) board[widthMinus1 - (x + 3)][y - 1] = board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < widthMinus3 && y >= 2) board[widthMinus1 - (x + 3)][y - 2] = board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < widthMinus2 && y >= 3) board[widthMinus1 - (x + 2)][y - 3] = board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < widthMinus1 && y >= 3) board[widthMinus1 - (x + 1)][y - 3] = board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) board[widthMinus1 - (x)][y - 3] = board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) board[widthMinus1 - (x - 1)][y - 3] = board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) board[widthMinus1 - (x - 2)][y - 3] = board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) board[widthMinus1 - (x - 3)][y - 2] = board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) board[widthMinus1 - (x - 3)][y - 1] = board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) board[widthMinus1 - (x - 3)][y] = board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < heightMinus1) board[widthMinus1 - (x - 3)][y + 1] = board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < heightMinus2) board[widthMinus1 - (x - 3)][y + 2] = board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        } else if (symmetry == Symmetry.ROTATIONAL) {
            if (x >= 2 && y < heightMinus3) board[widthMinus1 - (x - 2)][heightMinus1 - (y + 3)] = board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < heightMinus3) board[widthMinus1 - (x - 1)][heightMinus1 - (y + 3)] = board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < heightMinus3) board[widthMinus1 - (x)][heightMinus1 - (y + 3)] = board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < widthMinus1 && y < heightMinus3) board[widthMinus1 - (x + 1)][heightMinus1 - (y + 3)] = board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < widthMinus2 && y < heightMinus3) board[widthMinus1 - (x + 2)][heightMinus1 - (y + 3)] = board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < widthMinus3 && y < heightMinus2) board[widthMinus1 - (x + 3)][heightMinus1 - (y + 2)] = board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < widthMinus3 && y < heightMinus1) board[widthMinus1 - (x + 3)][heightMinus1 - (y + 1)] = board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < widthMinus3) board[widthMinus1 - (x + 3)][heightMinus1 - (y)] = board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < widthMinus3 && y >= 1) board[widthMinus1 - (x + 3)][heightMinus1 - (y - 1)] = board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < widthMinus3 && y >= 2) board[widthMinus1 - (x + 3)][heightMinus1 - (y - 2)] = board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < widthMinus2 && y >= 3) board[widthMinus1 - (x + 2)][heightMinus1 - (y - 3)] = board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < widthMinus1 && y >= 3) board[widthMinus1 - (x + 1)][heightMinus1 - (y - 3)] = board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) board[widthMinus1 - (x)][heightMinus1 - (y - 3)] = board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) board[widthMinus1 - (x - 1)][heightMinus1 - (y - 3)] = board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) board[widthMinus1 - (x - 2)][heightMinus1 - (y - 3)] = board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) board[widthMinus1 - (x - 3)][heightMinus1 - (y - 2)] = board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) board[widthMinus1 - (x - 3)][heightMinus1 - (y - 1)] = board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) board[widthMinus1 - (x - 3)][heightMinus1 - (y)] = board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < heightMinus1) board[widthMinus1 - (x - 3)][heightMinus1 - (y + 1)] = board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < heightMinus2) board[widthMinus1 - (x - 3)][heightMinus1 - (y + 2)] = board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        } else {
            if (x >= 2 && y < heightMinus3) board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < heightMinus3) board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < heightMinus3) board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < widthMinus1 && y < heightMinus3) board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < widthMinus2 && y < heightMinus3) board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < widthMinus3 && y < heightMinus2) board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < widthMinus3 && y < heightMinus1) board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < widthMinus3) board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < widthMinus3 && y >= 1) board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < widthMinus3 && y >= 2) board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < widthMinus2 && y >= 3) board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < widthMinus1 && y >= 3) board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < heightMinus1) board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < heightMinus2) board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        }
    }

    /**
     * Helper function to broadcast the current discovered symmetry to the BFS bots.
     * @param arrIdx the index at which the symmetry is written in the global array.
     * @throws GameActionException
     */
    public static void broadcastSymmetry(int arrIdx) throws GameActionException {
        int hash = 0;
        if (vertical) hash |= 1;
        if (horizontal) hash |= 1 << 1;
        if (rotational) hash |= 1 << 2;
    }

    public static void decodeSymmetry(int arrIdx) throws GameActionException {
        int hash = rc.readSharedArray(arrIdx);
        if ((hash & 1) == 0) vertical = false;
        if (((hash >>> 1) & 1) == 0) horizontal = false;
        if (((hash >>> 2) & 1) == 0) rotational = false;
    }

    public static void broadcastBfs() throws GameActionException {
        for (int i = 4; i < 64; ++i) {
            int hash = 0;
            hash |= optimal[internalIdx / height][internalIdx % height];
            if (++internalIdx == width * height) break;
            hash |= optimal[internalIdx / height][internalIdx % height] << 4;
            if (++internalIdx == width * height) break;
            hash |= optimal[internalIdx / height][internalIdx % height] << 8;
            if (++internalIdx == width * height) break;
            hash |= optimal[internalIdx / height][internalIdx % height] << 12;
            if (++internalIdx == width * height) break;
            rc.writeSharedArray(i, hash);
        }
        if (internalIdx == width * height) {
            switch (selfIdx) {
                case 45:
                    centreBfs = optimal;
                    break;
                case 46:
                    spawnBfs = optimal;
                    break;
                case 47:
                    flag0Bfs = optimal;
                    break;
                case 48:
                    flag1Bfs = optimal;
                    break;
                case 49:
                    flag2Bfs = optimal;
                    break;
            }
            bfsIdx = -3;
            internalIdx = 0;
        }
    }

    public static void receiveBfs() throws GameActionException {
        if (internalIdx == 0) {
            switch (bfsIdx) {
                case 45:
                    centreBfs = new int[width][height];
                    break;
                case 46:
                    spawnBfs = new int[width][height];
                    break;
                case 47:
                    flag0Bfs = new int[width][height];
                    break;
                case 48:
                    flag1Bfs = new int[width][height];
                    break;
                case 49:
                    flag2Bfs = new int[width][height];
                    break;
                default:
                    System.out.println("Something went wrong!");
            }
        }
        int[][] thisBfs;
        switch (bfsIdx) {
            case 45:
                thisBfs = centreBfs;
                break;
            case 46:
                thisBfs = spawnBfs;
                break;
            case 47:
                thisBfs = flag0Bfs;
                break;
            case 48:
                thisBfs = flag1Bfs;
                break;
            case 49:
                thisBfs = flag2Bfs;
                break;
            default:
                thisBfs = new int[width][height];
                System.out.println("Something went wrong!");
        }
        int mask = (1 << 4) - 1;
        for (int i = 4; i < 64; ++i) {
            int hash = rc.readSharedArray(i);
            thisBfs[internalIdx / height][internalIdx % height] = hash & mask;
            if (++internalIdx == width * height) break;
            thisBfs[internalIdx / height][internalIdx % height] = (hash >>> 4) & mask;
            if (++internalIdx == width * height) break;
            thisBfs[internalIdx / height][internalIdx % height] = (hash >>> 8) & mask;
            if (++internalIdx == width * height) break;
            thisBfs[internalIdx / height][internalIdx % height] = (hash >>> 12) & mask;
            if (++internalIdx == width * height) break;
        }
        if (internalIdx == width * height) {
            bfsIdx++;
            internalIdx = 0;
        }
    }

    public static void moveBfsUtil(int[][] bfsArr, BfsTarget target) throws GameActionException {
        if (!rc.isMovementReady()) {
            return;
        }
        MapLocation loc = rc.getLocation();
        RobotInfo[] friends = rc.senseNearbyRobots(-1, rc.getTeam());
        int i = bfsArr != null ? bfsArr[loc.x][loc.y] : 0;
        if (i > 0) {
            debug("Moving using bfs");
            Direction dir = directions[i - 1];
            rc.setIndicatorLine(rc.getLocation(), loc.add(dir), 0, 0, 255);
            Direction[] choices = {dir, dir.rotateLeft(), dir.rotateRight()};
            for (Direction choice: choices) {
                if (rc.canMove(choice)) {
                    rc.move(choice);
                    return;
                }
            }
            for (Direction choice: choices) {
                MapLocation next = loc.add(choice);
                int j = bfsArr != null ? bfsArr[next.x][next.y] : 0;
                if (j > 0) {
                    Direction nextDir = directions[j - 1];
                    for (Direction nextChoice: new Direction[]{nextDir, nextDir.rotateLeft(), nextDir.rotateRight()}) {
                        MapLocation nextNext = next.add(nextChoice);
                        if (rc.canDropFlag(nextNext)) {
                            RobotInfo friend = rc.senseRobotAtLocation(nextNext);
                            if (friend != null && friend.getTeam() == rc.getTeam()) {
                                rc.dropFlag(next);
                                debug("Passed the flag to a spot");
                                return;
                            }
                            System.out.println("No friend at " + nextNext.x + ", " + nextNext.y);
                        } else System.out.println("Can't drop flag at " + nextNext.x + ", " + nextNext.y);
                    }
                }
                if (rc.canDropFlag(next)) {
                    RobotInfo friend = rc.senseRobotAtLocation(next);
                    if (friend != null && friend.getTeam() == rc.getTeam()) {
                        rc.dropFlag(next);
                        debug("Passed the flag in bfs");
                        return;
                    }
                    System.out.println("No friend at " + next.x + ", " + next.y);
                } else System.out.println("Can't drop flag at " + next.x + ", " + next.y);
            }
        }
        debug("Can't bfs, defaulting");
        switch (target) {
            case CENTRE:
                moveBetter(centre);
                break;
            case SPAWN:
                moveBetter(closest(rc.getAllySpawnLocations()));
                break;
            case FLAG0:
            case FLAG1:
            case FLAG2:
                moveBetter(new MapLocation(1, 1));
                break;
            default:
                System.out.println("Something went wrong in moveBfsUtil()!");
        }
    }

    public static void moveBfs(BfsTarget target) throws GameActionException {
        switch (target) {
            case CENTRE:
                moveBfsUtil(centreBfs, target);
                break;
            case SPAWN:
                moveBfsUtil(spawnBfs, target);
                break;
            case FLAG0:
                moveBfsUtil(flag0Bfs, target);
                break;
            case FLAG1:
                moveBfsUtil(flag1Bfs, target);
                break;
            case FLAG2:
                moveBfsUtil(flag2Bfs, target);
                break;
            default:
                System.out.println("Something went wrong in moveBfs()!");
        }
    }

    public static void run(RobotController _rc) throws GameActionException {
        rc = _rc;
        rng = new Random(rc.getID()+1);
        MapLocation targetCell = new MapLocation(-1, -1);
        width = rc.getMapWidth();
        height = rc.getMapHeight();
        widthMinus1 = width - 1;
        widthMinus2 = width - 2;
        widthMinus3 = width - 3;
        heightMinus1 = height - 1;
        heightMinus2 = height - 2;
        heightMinus3 = height - 3;
        centre = new MapLocation(width / 2, height / 2);
        if (rc.getTeam() == Team.A) {
            team = 1;
        } else {
            team = 2;
        }
        board = new int[width][height];
        for (MapLocation spawn: rc.getAllySpawnLocations()) {
            board[spawn.x][spawn.y] = 3;
        }
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
                        if (ids[i]+9999 == rc.getID()) isBuilder = i;
                    }
                } else if (round >= 3 && round <= 160) {
                    if (rc.isSpawned()) {
                        if (selfIdx >= 45) {
                            for (int i = 4; i < 64; i += 2) {
                                decodeBroadcast(i);
                            }
                        } else {
                            recordVision();
                            if (selfIdx >= 2 && selfIdx <= 31) {
                                broadcastVision(selfIdx << 1);
                            }
                        }
                    }
                } else if (round == 161) {
                    if (selfIdx <= 44) {
                        // all robots broadcast their interpretation of the map's symmetry
                        broadcastSymmetry(selfIdx + 19);
                    } else {
                        // receive symmetry broadcasts from other robots
                        for (int i = 19; i < 64; ++i) {
                            decodeSymmetry(i);
                        }
                        if (symmetry == Symmetry.VERTICAL) {
                            getOpp = (x, y) -> board[x][heightMinus1 - y];
                        } else if (symmetry == Symmetry.HORIZONTAL) {
                            getOpp = (x, y) -> board[widthMinus1 - x][y];
                        } else {
                            getOpp = (x, y) -> board[widthMinus1 - x][heightMinus1 - y];
                        }
                    }
                    if (selfIdx >= 45) {
                        q = new StringBuilder();
                        MapLocation loc = new MapLocation(1, 1);  // will use Aiden's centre spawns once pulled
                        bfs = new int[width][height];
                        for (int x = 0; x < width; ++x) {
                            UnrolledUtils.fill(bfs[x], -1);
                        }
                        if (selfIdx == 45) {
                            q.append(hashLoc(centre));
                            bfs[centre.x][centre.y] = 0;
                        } else if (selfIdx == 46) {
                            for (MapLocation spawn: rc.getAllySpawnLocations()) {
                                q.append(hashLoc(spawn));
                                bfs[spawn.x][spawn.y] = 0;
                            }
                        } else if (selfIdx == 47) {
                            q.append(hashLoc(loc));
                        } else if (selfIdx == 48) {
                            q.append(hashLoc(loc));
                        } else if (selfIdx == 49) {
                            q.append(hashLoc(loc));
                        }
                        optimal = new int[width][height];
                        rc.writeSharedArray(selfIdx + 14, 0);
                        continue;
                    }
                } else if (round >= 162 && bfsIdx < 50) {
                    if (selfIdx >= 45 && !bfsDone) {
                        // BFS time baby!
                        while (q.length() > 0) {
                            int x1 = q.charAt(0) / height, y1 = q.charAt(0) % height;
                            q.deleteCharAt(0);
                            int nextDist = bfs[x1][y1] + 1;
                            for (int i = 0; i < 8; ++i) {
                                int x2 = x1 + directions[i].opposite().getDeltaX(), y2 = y1 + directions[i].opposite().getDeltaY();
                                if (x2 >= 0 && x2 < width && y2 >= 0 && y2 < height) {
                                    if ((board[x2][y2] | getOpp.applyAsInt(x2, y2)) == 0 && bfs[x2][y2] == -1) {
                                        bfs[x2][y2] = nextDist;
                                        optimal[x2][y2] = i + 1;
                                        q.append((char) (x2 * height + y2));
                                    }
                                }
                            }
                            if (Clock.getBytecodesLeft() < 1000) {
                                break;
                            }
                        }
                        if (q.length() > 0) {
                            continue;
                        } else {
                            bfsDone = true;
                            rc.writeSharedArray(selfIdx + 14, 1);
                            bfsIdx = -1;
                        }
                        continue;
                    }
                    if (bfsIdx == -2) {
                        bfsIdx = -1;
                    } else if (bfsIdx == -1) {
                        bfsIdx = 45;
                        for (int i = 59; i < 64; ++i) {
                            if (rc.readSharedArray(i) == 0) {
                                bfsIdx = -1;
                                break;
                            }
                        }
                        if (bfsIdx != -1) {
                            System.out.println(selfIdx + ", ready!");
                            if (bfsIdx == selfIdx) {
                                broadcastBfs();
                            } else if (bfsIdx < selfIdx) {
                                receiveBfs();
                            }
                        }
                    } else if (bfsIdx == selfIdx) {
                        broadcastBfs();
                    } else if (bfsIdx == -3) {
                        bfsIdx = selfIdx + 1;
                    } else {
                        receiveBfs();
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
                                for (Direction dir : shuffledDirections) {
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
                    if (isBuilder != 0){
                        if (!rc.getLocation().equals(spawnCentres[isBuilder-40])){
                            moveBetter(spawnCentres[isBuilder-40]);
                            rc.setIndicatorLine(rc.getLocation(), spawnCentres[isBuilder-40], 0, 0, 255);
                        }
                        if (rc.getLocation().equals(spawnCentres[isBuilder-40])) trapSpawn();
                        continue;
                    }
                    nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
                    MapLocation nextLoc;
                    MapLocation[] rawCrumbs = rc.senseNearbyCrumbs(-1);
                    List<MapLocation> validCrumbs = new ArrayList<MapLocation>();
                    for (MapLocation crumb : rawCrumbs) {
                        if (!rc.senseMapInfo(crumb).isWall()) validCrumbs.add(crumb);
                    }
                    MapLocation[] crumbs = validCrumbs.toArray(new MapLocation[0]);
                    if(crumbs.length > 0) {
                        // sort crumbs by distance
                        Arrays.sort(crumbs, closestComp());
                        nextLoc = crumbs[0];
                    } else {
                        if(movesLeft > 0 && !rc.getLocation().equals(target)) {
                            movesLeft--;
                        } else {
                            target = new MapLocation(StrictMath.max(0, StrictMath.min(width - 1, rc.getLocation().x + rng.nextInt(21) - 10)),
                                    StrictMath.max(0, StrictMath.min(height - 1, rc.getLocation().y + rng.nextInt(21) - 10)));
                            movesLeft = 7;
                        }
                        nextLoc = target;
                    }
                    moveBetter(nextLoc);
                } else if (!rc.hasFlag() && rc.senseNearbyFlags(0, rc.getTeam().opponent()).length >= 1 && !rc.canPickupFlag(rc.getLocation())) {
                    // wait, we need to pick up a flag dropped by a teammate
                } else {
                    if (rc.hasFlag()) {
                        moveBfs(BfsTarget.SPAWN);
                        continue;
                    }
                    if (isBuilder != 0){
                        if (!rc.getLocation().equals(spawnCentres[isBuilder-40])){
                            moveBetter(spawnCentres[isBuilder-40]);
                            rc.setIndicatorLine(rc.getLocation(), spawnCentres[isBuilder-40], 0, 0, 255);
                        }
                        if (rc.getLocation().equals(spawnCentres[isBuilder-40])) trapSpawn();
                        continue;
                    }
                    nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
                    pickupFlag(true);
                    // Find all triggered stun traps;
                    updateStuns();
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
                    int threshold = StrictMath.min(nearbyAllies.length * 75, 751) * (nearbyAllies.length + 1);
                    int enemyHP = 0;
                    RobotInfo[] rawEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                    List<RobotInfo> listEnemies = new ArrayList<RobotInfo>();
                    // sittingDucks contains all stunned enemies
                    List<RobotInfo> listSittingDucks = new ArrayList<RobotInfo>();
                    for(RobotInfo enemy : rawEnemies) {
                        boolean skip = false;
                        for (ActiveStun stun : activeStuns) {
                            if (enemy.getLocation().distanceSquaredTo(stun.location) <= 13) {
                                skip = true;
                                break;
                            }
                        }
                        if (skip) {
                            listSittingDucks.add(enemy);
                            rc.setIndicatorDot(enemy.getLocation(), 255, 255, 0);
                            continue;
                        }
                        enemyHP += enemy.health;
                        listEnemies.add(enemy);
                    }
                    RobotInfo[] enemies = listEnemies.toArray(new RobotInfo[0]);
                    sittingDucks = listSittingDucks.toArray(new RobotInfo[0]);
                    // Movement
                    {
                        if (enemyHP * 5 > nearbyHP * 2
                                && !rc.hasFlag()
                                && rc.senseNearbyFlags(9, rc.getTeam().opponent()).length == 0
                                && rc.senseNearbyFlags(4, rc.getTeam()).length == 0
                                && sittingDucks.length <= 0) {
                            // Begin Kiting
                            int currTargeted = 0;
                            Direction[] choices = new Direction[8];
                            MapLocation rcLocation = rc.getLocation();
                            boolean returnToCombat = rc.isActionReady() && rc.getHealth() > 750; // Only return if currently safe
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
                            for (Direction dir : shuffledDirections) {
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
                                if (rc.getHealth() < 700 && enemies.length > 0) {
                                    debug("RETREAT");
                                    RobotInfo nearestEnemy = enemies[0];
                                    int nearestEnemyDistSquared = 1000;
                                    for (RobotInfo enemy : enemies) {
                                        int currDistSquared = enemy.getLocation().distanceSquaredTo(rc.getLocation());
                                        if (currDistSquared < nearestEnemyDistSquared) {
                                            nearestEnemyDistSquared = currDistSquared;
                                            nearestEnemy = enemy;
                                        }
                                    }
                                    final RobotInfo actualNearestEnemy = nearestEnemy;
                                    Arrays.sort(choices, 0, n, (a, b) -> {
                                        return rc.getLocation().add(b).distanceSquaredTo(actualNearestEnemy.getLocation()) - rc.getLocation().add(a).distanceSquaredTo(actualNearestEnemy.getLocation());
                                    });
                                } else {
                                    Arrays.sort(choices, 0, n, (a, b) -> {
                                        return rc.getLocation().add(a).distanceSquaredTo(finalTargetCell) - rc.getLocation().add(b).distanceSquaredTo(finalTargetCell);
                                    });
                                }
                                rc.move(choices[0]);
                            } else {
                                // There are no choices
                                shuffle();
                                int minAdj = rc.senseNearbyRobots(2, rc.getTeam()).length;
                                Direction choice = Direction.NORTH;
                                boolean overridden = false;
                                for (Direction dir : shuffledDirections) {
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
                                    if (sittingDucks.length > 0) {
                                        rc.setIndicatorLine(rc.getLocation(), sittingDucks[0].getLocation(), 255, 0, 0);
                                        //System.out.println("Moving towards a sitting duck");
                                        moveBetter(sittingDucks[0].getLocation());
                                    } else {
                                        moveBetter(enemies[0].getLocation());
                                    }
                                }
                            }
                        } else if (sittingDucks.length > 0 && !rc.hasFlag()) {
                            int minDistSquared = 100;
                            MapLocation[] choices = new MapLocation[sittingDucks.length];
                            int n = 0;
                            for (RobotInfo enemy : sittingDucks) {
                                int distToEnemy = enemy.getLocation().distanceSquaredTo(rc.getLocation());
                                if (distToEnemy < minDistSquared) {
                                    minDistSquared = distToEnemy;
                                    n = 0;
                                }
                                if (distToEnemy == minDistSquared) {
                                    choices[n++] = enemy.getLocation();
                                }
                            }
                            MapLocation bestChoice = choices[0];
                            if (targetCell != null) {
                                int shortestDist = -1;
                                for (int i = 0; i < n; ++i) {
                                    MapLocation choice = choices[i];
                                    int distToTarget = choice.distanceSquaredTo(targetCell);
                                    if (shortestDist == -1 || distToTarget < shortestDist) {
                                        shortestDist = distToTarget;
                                        bestChoice = choice;
                                    }
                                }
                            }
                            rc.setIndicatorLine(rc.getLocation(), bestChoice, 255, 0, 0);
                            //System.out.println("Moving towards a sitting duck");
                            moveBetter(bestChoice);
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
