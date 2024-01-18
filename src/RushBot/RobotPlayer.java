package RushBot;

import battlecode.common.*;

import java.lang.System;
import java.util.*;
import java.util.function.ToIntBiFunction;

public strictfp class RobotPlayer {
    static void updateStuns() throws GameActionException {
        List<MapLocation> currStuns = new ArrayList<MapLocation>();
        MapInfo[] mapInfos = V.rc.senseNearbyMapInfos(-1);
        for (MapInfo mi : mapInfos) {
            if (mi.getTrapType() == TrapType.STUN || mi.getTrapType() == TrapType.EXPLOSIVE) {
                currStuns.add(mi.getMapLocation());
            }
        }
        for (MapLocation ml : V.prevStuns) {
            if (ml.distanceSquaredTo(V.rc.getLocation()) < 20) {
                boolean triggered = true;
                for (MapLocation ml2 : currStuns) {
                    if (ml2.equals(ml)) {
                        triggered = false;
                        break;
                    }
                }
                if (triggered) {
                    V.activeStuns.add(new ActiveStun(ml));
                }
            }
        }
        V.prevStuns = currStuns;
        debug("currStuns: " + currStuns.size());
        List<ActiveStun> newActiveStuns = new ArrayList<ActiveStun>();
        for (ActiveStun stun : V.activeStuns) {
            if (stun.updateRound()) {
                newActiveStuns.add(stun);
            }
        }
        V.activeStuns = newActiveStuns;
    }

    /**
     * Helper function to append to the bot's indicator string.
     * @param x the value to append.
     * @param <T> the type of the value.
     */
    static <T> void debug(T x) {
        V.indicatorString += String.valueOf(x) + "; ";
    }

    public static MapLocation unhashChar(char c) {
        return new MapLocation(c / V.height, c % V.height);
    }

    public static char hashLoc(MapLocation loc) {
        return (char) (loc.x * V.height + loc.y);
    }

    /**
     * Helper function to calculate the minimum distance to the edge of
     * the map
     * @param loc Location to find the smallest distance to the edge from
     * @return The smallest distance to the edge of the map
     */
    static int distFromEdge(MapLocation loc) {
        return StrictMath.min(StrictMath.min(loc.x, V.rc.getMapWidth() - loc.x), StrictMath.min(loc.y, V.rc.getMapHeight() - loc.y));
    }

    /**
     * Helper function to check if a friend can pick up a flag dropped in a direction.
     * @param dir the direction to drop the flag.
     * @return
     */
    static boolean canFriendPickup(Direction dir) throws GameActionException {
        MapLocation loc = V.rc.getLocation().add(dir);
        RobotInfo robot = V.rc.senseRobotAtLocation(loc);
        if (robot != null && robot.getTeam() == V.rc.getTeam()) {
            return true;
        }
        robot = V.rc.senseRobotAtLocation(loc.add(dir));
        if (robot != null && robot.getTeam() == V.rc.getTeam()) {
            return true;
        }
        robot = V.rc.senseRobotAtLocation(loc.add(dir.rotateRight()));
        if (robot != null && robot.getTeam() == V.rc.getTeam()) {
            return true;
        }
        robot = V.rc.senseRobotAtLocation(loc.add(dir.rotateLeft()));
        if (robot != null && robot.getTeam() == V.rc.getTeam()) {
            return true;
        }
        return false;
    }

    static MapLocation closest(MapLocation[] locs) {
        if(locs.length == 0) return new MapLocation(-1, -1);
        int mn = 1000000000;
        MapLocation res = locs[0];
        for (int i = 0; i < locs.length; i++) {
            if(!V.rc.onTheMap(locs[i])) continue;
            int dist = locs[i].distanceSquaredTo(V.rc.getLocation());
            if (dist < mn) {
                mn = dist;
                res = locs[i];
            }
        }
        return res;
    }

    static void moveBetter(MapLocation pos) throws GameActionException {
        if(V.stackSize != 0 && (!V.rc.getLocation().directionTo(pos).equals(V.stack[0]) && V.rc.canMove(V.rc.getLocation().directionTo(pos)) || V.rng.nextInt(32) == 0)) {
            debug("Stack reset");
            V.stackSize = 0;
        }
        if(V.stackSize == 0) {
            V.stack[V.stackSize++] = V.rc.getLocation().directionTo(pos);
        }
        if(V.stackSize == 1) {
            V.turnDir = V.rng.nextInt(2);
        }
        if(V.stackSize >= 2 && V.rc.canMove(V.stack[V.stackSize - 2])) {
            V.stackSize--;
        }
        boolean moveCooldownDone = V.rc.getMovementCooldownTurns() == 0;
        MapLocation nextLoc;
        RobotInfo nextLocRobot;
        boolean triedOtherDir = false;
        boolean hasFlag = V.rc.hasFlag() || V.rc.canPickupFlag(V.rc.getLocation());
        boolean fillableWater;
        while(V.stackSize < 8) {
            nextLoc = V.rc.getLocation().add(V.stack[V.stackSize - 1]);
            boolean allowFillNext = (nextLoc.x % 2) == (nextLoc.y % 2);
            boolean nearWall = false;
            boolean hasCrumbs = V.rc.onTheMap(nextLoc) && V.rc.senseMapInfo(nextLoc).getCrumbs() > 0;
            for (MapInfo mi : V.rc.senseNearbyMapInfos(nextLoc, 2)) {
                if (mi.isWall() || mi.isDam()) {
                    nearWall = true;
                    break;
                }
            }
            fillableWater = (nearWall || allowFillNext || hasCrumbs) && V.rc.canFill(nextLoc);
            if(V.rc.onTheMap(nextLoc)) {
                if(!moveCooldownDone) {
                    // if it's not a wall, and if there's water we can fill it
                    if (!V.rc.senseMapInfo(nextLoc).isWall() && (fillableWater || !hasFlag)) {
                        break;
                    }
                } else {
                    // if we can move there, or if we can fill it
                    if (V.rc.canMove(V.stack[V.stackSize - 1]) || (fillableWater && !hasFlag)) {
                        break;
                    }
                }
                nextLocRobot = V.rc.senseRobotAtLocation(nextLoc);
                // otherwise, if we have the flag and the square we're trying to move to is obstructed by a friend
                if (hasFlag && nextLocRobot != null && nextLocRobot.getTeam() == V.rc.getTeam()) {
                    debug("Passing flag");
                    break;
                }
            } else {
                // reset if hugging wall, try other turn dir
                V.stackSize = 1;
                if(triedOtherDir) {
                    break;
                }
                V.turnDir = 1 - V.turnDir;
                triedOtherDir = true;
            }
            V.stack[V.stackSize] = V.turnDir == 0 ? V.stack[V.stackSize - 1].rotateLeft() : V.stack[V.stackSize - 1].rotateRight();
            V.stackSize++;
        }
        if (V.stackSize >= 8) {
            V.stackSize = 1;
        }
        Direction dir = V.stack[V.stackSize - 1];
        nextLoc = V.rc.getLocation().add(dir);
        boolean allowFillNext = (nextLoc.x % 2) == (nextLoc.y % 2);
        boolean nearWall = false;
        boolean hasCrumbs = V.rc.senseNearbyCrumbs(0).length > 0;
        for (MapInfo mi : V.rc.senseNearbyMapInfos(nextLoc, 2)) {
            if (mi.isWall() || mi.isDam() || hasCrumbs) {
                nearWall = true;
                break;
            }
        }
        if (V.rc.canFill(nextLoc) && !hasFlag) {
            V.rc.fill(nextLoc);
        }
        if(V.rc.canMove(dir)) {
            V.rc.move(dir);
        }
        nextLoc = V.rc.getLocation().add(dir);
        if(V.rc.onTheMap(nextLoc)) {
            nextLocRobot = V.rc.senseRobotAtLocation(nextLoc);
            if(V.rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.getTeam() == V.rc.getTeam()) {
                V.rc.dropFlag(nextLoc);
                debug("Passed flag in moveBetter()");
                writeStack();
            }
        }
    }
    static int stackPassIndex = 3;
    static void writeStack() throws GameActionException {
        // 3 bits for stack size, 1 bit for turn dir, 3 bits for first dir;
        if(V.stackSize == 0) {
            V.rc.writeSharedArray(stackPassIndex, 0);
            return;
        }
        for(int i = 0; i < 8; i++) {
            if(V.stack[0] == Direction.values()[i]) {
                V.rc.writeSharedArray(stackPassIndex, (V.stackSize << 4) + (V.turnDir << 3) + i);
                return;
            }
        }
    }
    static void readStack() throws GameActionException {
        int data = V.rc.readSharedArray(stackPassIndex);
        if(data == 0) {
            return;
        }
        V.stackSize = data >> 4;
        V.turnDir = (data >> 3) & 1;
        V.stack[0] = Direction.values()[data & 7];
        for(int i = 1; i < V.stackSize; i++) {
            V.stack[i] = V.turnDir == 0 ? V.stack[i - 1].rotateLeft() : V.stack[i - 1].rotateRight();
        }
        V.rc.writeSharedArray(stackPassIndex, 0);
    }

    static void broadcastSwarmTarget(MapLocation loc) throws GameActionException {
        V.rc.writeSharedArray(0, V.rc.getID());
        V.rc.writeSharedArray(1, loc.x);
        V.rc.writeSharedArray(2, loc.y);
    }

    static MapLocation swarmTarget;
    static int swarmEnd = 0;
    static MapLocation findTarget() throws GameActionException {
        int swarmLeader = V.rc.readSharedArray(0);
        if(swarmLeader == V.rc.getID()) {
            V.rc.writeSharedArray(0, 0);
        }
        if(V.rc.hasFlag()) return closest(V.rc.getAllySpawnLocations());
        FlagInfo[] friendlyFlags = V.rc.senseNearbyFlags(-1, V.rc.getTeam());
        if(friendlyFlags.length > 0 && V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent()).length > 0) {
            for(FlagInfo f : friendlyFlags) {
                if(V.rc.senseNearbyRobots(f.getLocation(), 0, V.rc.getTeam().opponent()).length > 0) {
                    broadcastSwarmTarget(f.getLocation());
                    return f.getLocation();
                }
            }
        }
        FlagInfo[] possibleFlags = V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent());
        MapLocation[] flagLocs = new MapLocation[possibleFlags.length];
        for(int i = 0; i < possibleFlags.length; i++) {
            if(!possibleFlags[i].isPickedUp()) flagLocs[i] = possibleFlags[i].getLocation();
            else flagLocs[i] = new MapLocation(-1, -1);
        }
        MapLocation closestFlag = closest(flagLocs);
        if(V.rc.onTheMap(closestFlag)) return closestFlag;
        RobotInfo[] enemies = V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent());
        MapLocation[] enemyLocs = new MapLocation[enemies.length];
        for(int i = 0; i < enemies.length; i++) {
            enemyLocs[i] = enemies[i].getLocation();
        }
        if(enemies.length >= 1) {
            return closest(enemyLocs);
        }
        if(swarmLeader != 0) {
            MapLocation newSwarmTarget = new MapLocation(V.rc.readSharedArray(1), V.rc.readSharedArray(2));
            if(V.rc.getLocation().distanceSquaredTo(swarmTarget) < StrictMath.max(V.height, V.width)) {
                swarmTarget = newSwarmTarget;
                swarmEnd = V.rc.getRoundNum() + StrictMath.max(V.height, V.width) / 2;
            }
        }
        if(swarmEnd < V.rc.getRoundNum()) swarmTarget = new MapLocation(-1, -1);
        if(V.rc.onTheMap(swarmTarget)) return swarmTarget;
        // System.out.println("Swarm retargeted");
        MapLocation[] possibleCrumbs = V.rc.senseNearbyCrumbs(-1);
        if(possibleCrumbs.length >= 1) return closest(possibleCrumbs);
        MapLocation[] possibleSenses = V.rc.senseBroadcastFlagLocations();
        Arrays.sort(possibleSenses, (MapLocation a, MapLocation b) -> {
                return b.distanceSquaredTo(V.rc.getLocation()) - a.distanceSquaredTo(V.rc.getLocation());
            }); // yes this is supposed to be sorted furthest first
        if(possibleSenses.length > 0) {
            swarmTarget = possibleSenses[(int)Math.sqrt(V.rng.nextInt(possibleSenses.length * possibleSenses.length))];
            swarmEnd = V.rc.getRoundNum() + StrictMath.max(V.height, V.width) / 2;
            return swarmTarget;
        }
        return V.centre;
    }
    static void attack() throws GameActionException {
        RobotInfo[] possibleEnemies = V.rc.senseNearbyRobots(4, V.rc.getTeam().opponent());
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
                if(V.rc.canAttack(enemy.getLocation())
                        && (totalPriority > currPriority
                        || (totalPriority == currPriority && enemy.health < attackTarget.health))) {
                    attackTarget = enemy;
                    currPriority = totalPriority;
                }
            }
            if(V.rc.canAttack(attackTarget.getLocation())) {
                V.rc.attack(attackTarget.getLocation());
            }
        }
    }
    static void healFlagBearer() throws GameActionException {
        RobotInfo[] nearby = V.rc.senseNearbyRobots();
        for (RobotInfo robot: nearby) {
            if (robot.hasFlag() && robot.getTeam() == V.rc.getTeam()) {
                MapLocation loc = robot.getLocation();
                if (V.rc.canHeal(loc)) {
                    V.rc.heal(loc);
                    return;
                }
            }
        }
    }
    static void heal() throws GameActionException {
        // Also prioritise flag carriers when healing
        RobotInfo[] nearbyAllyRobots = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
        RobotInfo healTarget = V.rc.senseRobot(V.rc.getID());
        for (RobotInfo ally : nearbyAllyRobots) {
            if(V.rc.canHeal(ally.getLocation())
               && ally.health < healTarget.health) {
                healTarget = ally;
            }
        }
        if(V.rc.canHeal(healTarget.getLocation())) {
            V.rc.heal(healTarget.getLocation());
        }
    }

    static void attackOrHeal() throws GameActionException {
        RobotInfo[] nearby = V.rc.senseNearbyRobots();
        healFlagBearer();
        for (RobotInfo robot: nearby) {
            if (robot.hasFlag() && robot.getTeam() == V.rc.getTeam().opponent()) {
                MapLocation loc = robot.getLocation();
                if (V.rc.canAttack(loc)) {
                    V.rc.attack(loc);
                    return;
                }
            }
        }
        Arrays.sort(nearby, (a, b) -> { return a.getHealth() - b.getHealth(); });
        for (RobotInfo robot: nearby) {
            MapLocation loc = robot.getLocation();
            if (robot.getTeam() == V.rc.getTeam()) {
                if (V.rc.canHeal(loc)) {
                    V.rc.setIndicatorLine(V.rc.getLocation(), loc, 0, 255, 0);
                    V.rc.heal(loc);
                    return;
                }
            } else {
                if (V.rc.canAttack(loc)) {
                    V.rc.setIndicatorLine(V.rc.getLocation(), loc, 255, 0, 0);
                    V.rc.attack(loc);
                    return;
                }
            }
        }
    }

    static void farmBuildXp(int level) throws GameActionException {
        if(V.rc.getLevel(SkillType.BUILD) < level) {
            for(Direction d : V.directions) {
                if((V.rc.adjacentLocation(d).x % 2) == (V.rc.adjacentLocation(d).y % 2)) continue;
                if(V.rc.canDig(V.rc.getLocation().add(d))) V.rc.dig(V.rc.getLocation().add(d));
            }
        }
    }

    public static void pickupFlagUtil(FlagInfo flag) throws GameActionException {
        MapLocation flagLoc = flag.getLocation();
        if (!V.rc.canPickupFlag(flagLoc)) {
            return;
        }
        if (flagLoc.equals(V.rc.getLocation())) {
            V.rc.pickupFlag(flagLoc);
            return;
        }
        int i = V.spawnBfs != null ? V.spawnBfs[flagLoc.x][flagLoc.y] : 0;
        if (i > 0) {
            Direction dir = V.directions[i - 1];
            for (Direction choice: new Direction[]{dir, dir.rotateLeft(), dir.rotateRight()}) {
                MapLocation next = flagLoc.add(choice);
                if (next.equals(V.rc.getLocation())) {
                    V.rc.pickupFlag(flagLoc);
                    readStack();
                    return;
                }
                RobotInfo friend = V.rc.senseRobotAtLocation(next);
                if (friend != null && friend.getTeam() == V.rc.getTeam()) {
                    debug("Letting friend pickup");
                    return;
                }
            }
        }
        V.rc.pickupFlag(flagLoc);
        readStack();
    }

    static void pickupFlag(boolean allowCurrentCell) throws GameActionException {
        if(V.rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
            for(FlagInfo f : V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent())) {
                pickupFlagUtil(f);
            }
        }
    }

    static void tryBuildTrap(MapLocation loc, RobotInfo[] visibleEnemies, int nearbyTraps, boolean ok) throws GameActionException {
        TrapType chosenTrap = V.rng.nextInt((visibleEnemies.length<=2)?5:2) == 0 ? TrapType.STUN : TrapType.EXPLOSIVE;
        boolean adjTrap = false;
        boolean veryCloseTrap = false;
        if(!V.rc.onTheMap(loc)) return;
        for(MapInfo m : V.rc.senseNearbyMapInfos(loc, 5)) {
            if(m.getTrapType() != TrapType.NONE) {
                adjTrap = true;
                if (m.getMapLocation().distanceSquaredTo(loc) <= 2) {
                    veryCloseTrap = true;
                }
            }
        }
        // CR in this context is chance reciprocal
        int wallCR = (ok)?0:100; // Instead of outright cancel, make it a weighting
        int nearbyTrapCR = 50+nearbyTraps*100;
        int dissuadeEdgeCR = 71 - 10 * StrictMath.min(distFromEdge(V.rc.getLocation()), 7);
        int nearbyEnemiesCR = StrictMath.max(100 - (50 * visibleEnemies.length), 1);
        int chanceReciprocal = StrictMath.min(nearbyTrapCR, nearbyEnemiesCR) + wallCR;// + dissuadeEdgeCR;
        if((!veryCloseTrap || chosenTrap == TrapType.EXPLOSIVE) && (!adjTrap || V.rc.getCrumbs() > 5000 || nearbyEnemiesCR <= 2) && V.rc.canBuild(chosenTrap, loc) && V.rng.nextInt(chanceReciprocal) == 0) {
            V.rc.build(chosenTrap, loc);
        }
    }

    static void buildTraps() throws GameActionException {
        boolean ok = true;
        MapInfo[] mapInfos = V.rc.senseNearbyMapInfos(3);
        for(MapInfo m : mapInfos) {
            if(m.isWall()) {
                ok = false;
                break;
            }
        }
        for(MapInfo m : V.rc.senseNearbyMapInfos(2)) {
            if(m.isSpawnZone() && m.getSpawnZoneTeam() != V.rc.getTeam()) {
                ok = true;
            }
        }
        //if(!ok) return; // turned into weighting, see below
        if(V.rc.getCrumbs() >= 250 && V.rc.getRoundNum() >= 180) {
            RobotInfo[] visibleEnemies = V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent());
            if(visibleEnemies.length == 0) return;
            // Calculate number of nearby traps
            int nearbyTraps = 0;
            for (MapInfo mi : mapInfos) {
                if (mi.getTrapType() != TrapType.NONE) {
                    ++nearbyTraps;
                }
            }
            for(Direction d : Direction.values()) {
                if(V.rc.senseMapInfo(V.rc.adjacentLocation(d)).isWater()) continue;
                tryBuildTrap(V.rc.adjacentLocation(d), visibleEnemies, nearbyTraps, ok);
            }
            for(Direction d : Direction.values()) {
                tryBuildTrap(V.rc.adjacentLocation(d), visibleEnemies, nearbyTraps, ok);
            }
        }
    }

    static void shuffle() {
        for (int i = 7; i > 0; --i) {
            int j = V.rng.nextInt(i + 1);
            Direction temp = V.shuffledDirections[i];
            V.shuffledDirections[i] = V.shuffledDirections[j];
            V.shuffledDirections[j] = temp;
        }
    }

    static boolean isCentreSpawn(MapLocation loc) throws GameActionException {
        for (Direction dir: V.directions) {
            if (!V.rc.canSenseLocation(loc.add(dir))) {
                return false;
            }
            if (!V.rc.senseMapInfo(loc.add(dir)).isSpawnZone()) {
                return false;
            }
        }
        return true;
    }

    static void trapSpawn() throws GameActionException {
        for (int i=0; i<3; i++){
            if (V.rc.canBuild(TrapType.STUN, V.rc.getLocation().add(V.trapDirs[i]))) {
                V.rc.build(TrapType.STUN, V.rc.getLocation().add(V.trapDirs[i]));
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
        return closestComp(V.rc.getLocation());
    }

    static void printBoard() {
        for (int y = V.height - 1; y >= 0; --y) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < V.width; ++x) {
//                if (optimal[x][y] < 10) row.append(' ');
//                if (optimal[x][y] < 0) row.append(' ');
//                else row.append(optimal[x][y]);
                row.append(" ");
                if (V.spawnBfs[x][y] == 0) row.append(" ");
                else row.append(V.dirStrs[V.spawnBfs[x][y] - 1]);
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
        if (V.symmetry == V.Symmetry.HORIZONTAL) {
            V.board[V.width - 1 - x][y] = V.board[x][y];
        }
        if (V.symmetry == V.Symmetry.VERTICAL) {
            V.board[x][V.height - 1 - y] = V.board[x][y];
        }
        if (V.symmetry == V.Symmetry.ROTATIONAL) {
            V.board[V.width - 1 - x][V.height - 1 - y] = V.board[x][y];
        }
    }

    /**
     * Records the squares in the vision radius seen by the current bot.
     */
    static void recordVision() {
        for (MapInfo square: V.rc.senseNearbyMapInfos()) {
            int x = square.getMapLocation().x;
            int y = square.getMapLocation().y;
            if (V.board[x][y] == 0) {
                if (square.getSpawnZoneTeam() == V.rc.getTeam()) {
                    V.board[x][y] = 3;
                } else if (square.getSpawnZoneTeam() == V.rc.getTeam().opponent()) {
                    V.board[x][y] = 4;
                } else if (square.isWall()) {
                    V.board[x][y] = 2;
                } else if (!square.isWater() && !square.isPassable()) {
                    // dam
                    V.board[x][y] = 5;
                } else {
                    V.board[x][y] = 1;
                }

                if (V.symmetry == V.Symmetry.UNKNOWN) {
                    int ver = V.board[x][V.height - 1 - y];
                    if (!sameTile(V.board[x][y], ver)) {
                        V.vertical = false;
                        if (!V.horizontal) {
                            V.symmetry = V.Symmetry.ROTATIONAL;
                        } else if (!V.rotational) {
                            V.symmetry = V.Symmetry.HORIZONTAL;
                        }
                    }
                    int hor = V.board[V.width - 1 - x][y];
                    if (!sameTile(V.board[x][y], hor)) {
                        V.horizontal = false;
                        if (!V.vertical) {
                            V.symmetry = V.Symmetry.ROTATIONAL;
                        } else if (!V.rotational) {
                            V.symmetry = V.Symmetry.VERTICAL;
                        }
                    }
                    int rot = V.board[V.width - 1 - x][V.height - 1 - y];
                    if (!sameTile(V.board[x][y], rot)) {
                        V.rotational = false;
                        if (!V.horizontal) {
                            V.symmetry = V.Symmetry.VERTICAL;
                        } else if (!V.vertical) {
                            V.symmetry = V.Symmetry.HORIZONTAL;
                        }
                    }
                } else {
                    updateCellSymmetry(x, y);
                }
            }
        }
    }

    public static void broadcastVision(int arrayIdx) throws GameActionException {
        int x = V.rc.getLocation().x, y = V.rc.getLocation().y;
        int hash1 = x * V.height + y, hash2 = 0;
        V.rc.writeSharedArray(arrayIdx, hash1);
        MapLocation loc;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-2, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-1, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 1;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(0, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 2;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(1, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 3;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(2, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 4;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, 2)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 5;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, 1)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 6;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, 0)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 7;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, -1)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 8;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, -2)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 9;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(2, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 10;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(1, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 11;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(0, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 12;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-1, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 13;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-2, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 14;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, -2)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 15;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, -1)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 12;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, 0)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 13;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, 1)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 14;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, 2)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 15;
        V.rc.writeSharedArray(arrayIdx, hash1);
        V.rc.writeSharedArray(arrayIdx | 1, hash2);
    }

    public static void decodeBroadcast(int arrIdx) throws GameActionException {
        int hash1 = V.rc.readSharedArray(arrIdx), hash2 = V.rc.readSharedArray(arrIdx | 1);
        int locHash = hash1 & ((1 << 12) - 1);
        int x = locHash / V.height, y = locHash % V.height;
        if (V.symmetry == V.Symmetry.VERTICAL) {
            if (x >= 2 && y < V.heightMinus3) V.board[x - 2][V.heightMinus1 - (y + 3)] = V.board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < V.heightMinus3) V.board[x - 1][V.heightMinus1 - (y + 3)] = V.board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < V.heightMinus3) V.board[x][V.heightMinus1 - (y + 3)] = V.board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < V.widthMinus1 && y < V.heightMinus3) V.board[x + 1][V.heightMinus1 - (y + 3)] = V.board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < V.widthMinus2 && y < V.heightMinus3) V.board[x + 2][V.heightMinus1 - (y + 3)] = V.board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus2) V.board[x + 3][V.heightMinus1 - (y + 2)] = V.board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus1) V.board[x + 3][V.heightMinus1 - (y + 1)] = V.board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < V.widthMinus3) V.board[x + 3][V.heightMinus1 - (y)] = V.board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < V.widthMinus3 && y >= 1) V.board[x + 3][V.heightMinus1 - (y - 1)] = V.board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < V.widthMinus3 && y >= 2) V.board[x + 3][V.heightMinus1 - (y - 2)] = V.board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < V.widthMinus2 && y >= 3) V.board[x + 2][V.heightMinus1 - (y - 3)] = V.board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < V.widthMinus1 && y >= 3) V.board[x + 1][V.heightMinus1 - (y - 3)] = V.board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) V.board[x][V.heightMinus1 - (y - 3)] = V.board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) V.board[x - 1][V.heightMinus1 - (y - 3)] = V.board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) V.board[x - 2][V.heightMinus1 - (y - 3)] = V.board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) V.board[x - 3][V.heightMinus1 - (y - 2)] = V.board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) V.board[x - 3][V.heightMinus1 - (y - 1)] = V.board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) V.board[x - 3][V.heightMinus1 - (y)] = V.board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < V.heightMinus1) V.board[x - 3][V.heightMinus1 - (y + 1)] = V.board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < V.heightMinus2) V.board[x - 3][V.heightMinus1 - (y + 2)] = V.board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        } else if (V.symmetry == V.Symmetry.HORIZONTAL) {
            if (x >= 2 && y < V.heightMinus3) V.board[V.widthMinus1 - (x - 2)][y + 3] = V.board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < V.heightMinus3) V.board[V.widthMinus1 - (x - 1)][y + 3] = V.board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < V.heightMinus3) V.board[V.widthMinus1 - (x)][y + 3] = V.board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < V.widthMinus1 && y < V.heightMinus3) V.board[V.widthMinus1 - (x + 1)][y + 3] = V.board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < V.widthMinus2 && y < V.heightMinus3) V.board[V.widthMinus1 - (x + 2)][y + 3] = V.board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus2) V.board[V.widthMinus1 - (x + 3)][y + 2] = V.board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus1) V.board[V.widthMinus1 - (x + 3)][y + 1] = V.board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < V.widthMinus3) V.board[V.widthMinus1 - (x + 3)][y] = V.board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < V.widthMinus3 && y >= 1) V.board[V.widthMinus1 - (x + 3)][y - 1] = V.board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < V.widthMinus3 && y >= 2) V.board[V.widthMinus1 - (x + 3)][y - 2] = V.board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < V.widthMinus2 && y >= 3) V.board[V.widthMinus1 - (x + 2)][y - 3] = V.board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < V.widthMinus1 && y >= 3) V.board[V.widthMinus1 - (x + 1)][y - 3] = V.board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) V.board[V.widthMinus1 - (x)][y - 3] = V.board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) V.board[V.widthMinus1 - (x - 1)][y - 3] = V.board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) V.board[V.widthMinus1 - (x - 2)][y - 3] = V.board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) V.board[V.widthMinus1 - (x - 3)][y - 2] = V.board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) V.board[V.widthMinus1 - (x - 3)][y - 1] = V.board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) V.board[V.widthMinus1 - (x - 3)][y] = V.board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < V.heightMinus1) V.board[V.widthMinus1 - (x - 3)][y + 1] = V.board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < V.heightMinus2) V.board[V.widthMinus1 - (x - 3)][y + 2] = V.board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        } else if (V.symmetry == V.Symmetry.ROTATIONAL) {
            if (x >= 2 && y < V.heightMinus3) V.board[V.widthMinus1 - (x - 2)][V.heightMinus1 - (y + 3)] = V.board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < V.heightMinus3) V.board[V.widthMinus1 - (x - 1)][V.heightMinus1 - (y + 3)] = V.board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < V.heightMinus3) V.board[V.widthMinus1 - (x)][V.heightMinus1 - (y + 3)] = V.board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < V.widthMinus1 && y < V.heightMinus3) V.board[V.widthMinus1 - (x + 1)][V.heightMinus1 - (y + 3)] = V.board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < V.widthMinus2 && y < V.heightMinus3) V.board[V.widthMinus1 - (x + 2)][V.heightMinus1 - (y + 3)] = V.board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus2) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y + 2)] = V.board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus1) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y + 1)] = V.board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < V.widthMinus3) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y)] = V.board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < V.widthMinus3 && y >= 1) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y - 1)] = V.board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < V.widthMinus3 && y >= 2) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y - 2)] = V.board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < V.widthMinus2 && y >= 3) V.board[V.widthMinus1 - (x + 2)][V.heightMinus1 - (y - 3)] = V.board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < V.widthMinus1 && y >= 3) V.board[V.widthMinus1 - (x + 1)][V.heightMinus1 - (y - 3)] = V.board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) V.board[V.widthMinus1 - (x)][V.heightMinus1 - (y - 3)] = V.board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) V.board[V.widthMinus1 - (x - 1)][V.heightMinus1 - (y - 3)] = V.board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) V.board[V.widthMinus1 - (x - 2)][V.heightMinus1 - (y - 3)] = V.board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y - 2)] = V.board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y - 1)] = V.board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y)] = V.board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < V.heightMinus1) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y + 1)] = V.board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < V.heightMinus2) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y + 2)] = V.board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        } else {
            if (x >= 2 && y < V.heightMinus3) V.board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < V.heightMinus3) V.board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < V.heightMinus3) V.board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < V.widthMinus1 && y < V.heightMinus3) V.board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < V.widthMinus2 && y < V.heightMinus3) V.board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus2) V.board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus1) V.board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < V.widthMinus3) V.board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < V.widthMinus3 && y >= 1) V.board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < V.widthMinus3 && y >= 2) V.board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < V.widthMinus2 && y >= 3) V.board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < V.widthMinus1 && y >= 3) V.board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) V.board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) V.board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) V.board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) V.board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) V.board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) V.board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < V.heightMinus1) V.board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < V.heightMinus2) V.board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        }
    }

    /**
     * Helper function to broadcast the current discovered symmetry to the BFS bots.
     * @param arrIdx the index at which the symmetry is written in the global array.
     * @throws GameActionException
     */
    public static void broadcastSymmetry(int arrIdx) throws GameActionException {
        int hash = 0;
        if (V.vertical) hash |= 1;
        if (V.horizontal) hash |= 1 << 1;
        if (V.rotational) hash |= 1 << 2;
    }

    public static void decodeSymmetry(int arrIdx) throws GameActionException {
        int hash = V.rc.readSharedArray(arrIdx);
        if ((hash & 1) == 0) V.vertical = false;
        if (((hash >>> 1) & 1) == 0) V.horizontal = false;
        if (((hash >>> 2) & 1) == 0) V.rotational = false;
    }

    public static void broadcastBfs() throws GameActionException {
        for (int i = 4; i < 64; ++i) {
            int hash = 0;
            hash |= V.optimal[V.internalIdx / V.height][V.internalIdx % V.height];
            if (++V.internalIdx == V.width * V.height) break;
            hash |= V.optimal[V.internalIdx / V.height][V.internalIdx % V.height] << 4;
            if (++V.internalIdx == V.width * V.height) break;
            hash |= V.optimal[V.internalIdx / V.height][V.internalIdx % V.height] << 8;
            if (++V.internalIdx == V.width * V.height) break;
            hash |= V.optimal[V.internalIdx / V.height][V.internalIdx % V.height] << 12;
            if (++V.internalIdx == V.width * V.height) break;
            V.rc.writeSharedArray(i, hash);
        }
        if (V.internalIdx == V.width * V.height) {
            switch (V.selfIdx) {
                case 45:
                    V.centreBfs = V.optimal;
                    break;
                case 46:
                    V.spawnBfs = V.optimal;
                    break;
                case 47:
                    V.flag0Bfs = V.optimal;
                    break;
                case 48:
                    V.flag1Bfs = V.optimal;
                    break;
                case 49:
                    V.flag2Bfs = V.optimal;
                    break;
            }
            V.bfsIdx = -3;
            V.internalIdx = 0;
        }
    }

    public static void receiveBfs() throws GameActionException {
        if (V.internalIdx == 0) {
            switch (V.bfsIdx) {
                case 45:
                    V.centreBfs = new int[V.width][V.height];
                    break;
                case 46:
                    V.spawnBfs = new int[V.width][V.height];
                    break;
                case 47:
                    V.flag0Bfs = new int[V.width][V.height];
                    break;
                case 48:
                    V.flag1Bfs = new int[V.width][V.height];
                    break;
                case 49:
                    V.flag2Bfs = new int[V.width][V.height];
                    break;
                default:
                    System.out.println("Something went wrong!");
            }
        }
        int[][] thisBfs;
        switch (V.bfsIdx) {
            case 45:
                thisBfs = V.centreBfs;
                break;
            case 46:
                thisBfs = V.spawnBfs;
                break;
            case 47:
                thisBfs = V.flag0Bfs;
                break;
            case 48:
                thisBfs = V.flag1Bfs;
                break;
            case 49:
                thisBfs = V.flag2Bfs;
                break;
            default:
                thisBfs = new int[V.width][V.height];
                System.out.println("Something went wrong!");
        }
        int mask = (1 << 4) - 1;
        for (int i = 4; i < 64; ++i) {
            int hash = V.rc.readSharedArray(i);
            thisBfs[V.internalIdx / V.height][V.internalIdx % V.height] = hash & mask;
            if (++V.internalIdx == V.width * V.height) break;
            thisBfs[V.internalIdx / V.height][V.internalIdx % V.height] = (hash >>> 4) & mask;
            if (++V.internalIdx == V.width * V.height) break;
            thisBfs[V.internalIdx / V.height][V.internalIdx % V.height] = (hash >>> 8) & mask;
            if (++V.internalIdx == V.width * V.height) break;
            thisBfs[V.internalIdx / V.height][V.internalIdx % V.height] = (hash >>> 12) & mask;
            if (++V.internalIdx == V.width * V.height) break;
        }
        if (V.internalIdx == V.width * V.height) {
            V.bfsIdx++;
            V.internalIdx = 0;
        }
    }

    public static void moveBfsUtil(int[][] bfsArr, V.BfsTarget target) throws GameActionException {
        //printBoard();
        if (!V.rc.isMovementReady()) {
            return;
        }
        MapLocation loc = V.rc.getLocation();
        RobotInfo[] friends = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
        int i = bfsArr != null ? bfsArr[loc.x][loc.y] : 0;
        if (i > 0) {
            debug("Moving using bfs");
            System.out.println("Moving using bfs");
            Direction dir = V.directions[i - 1];
            V.rc.setIndicatorLine(V.rc.getLocation(), loc.add(dir), 0, 0, 255);
            Direction[] choices = i - 1 < 4 ? new Direction[]{dir, dir.rotateLeft(), dir.rotateRight()} : new Direction[]{dir};
            for (Direction choice: choices) {
                if (V.rc.canMove(choice)) {
                    V.rc.move(choice);
                    System.out.println("Moved to " + V.rc.getLocation().x + ", " + V.rc.getLocation().y + ": " + i);
                    return;
                }
            }
            for (Direction choice: choices) {
                MapLocation next = loc.add(choice);
                if(!V.rc.onTheMap(next)) continue;
                int j = bfsArr[next.x][next.y];
                if (j > 0) {
                    Direction nextDir = V.directions[j - 1];
                    for (Direction nextChoice: new Direction[]{nextDir, nextDir.rotateLeft(), nextDir.rotateRight()}) {
                        MapLocation nextNext = next.add(nextChoice);
                        if (V.rc.canDropFlag(nextNext)) {
                            RobotInfo friend = V.rc.senseRobotAtLocation(nextNext);
                            if (friend != null && friend.getTeam() == V.rc.getTeam()) {
                                V.rc.dropFlag(next);
                                debug("Passed the flag to a spot");
                                return;
                            }
                            System.out.println("No friend at " + nextNext.x + ", " + nextNext.y);
                        } else System.out.println("Can't drop flag at " + nextNext.x + ", " + nextNext.y);
                    }
                }
                if (V.rc.canDropFlag(next)) {
                    RobotInfo friend = V.rc.senseRobotAtLocation(next);
                    if (friend != null && friend.getTeam() == V.rc.getTeam()) {
                        V.rc.dropFlag(next);
                        debug("Passed the flag in bfs");
                        return;
                    }
                    System.out.println("No friend at " + next.x + ", " + next.y);
                } else System.out.println("Can't drop flag at " + next.x + ", " + next.y);
            }
        }
        System.out.println("Can't bfs, defaulting");
        switch (target) {
            case CENTRE:
                moveBetter(V.centre);
                break;
            case SPAWN:
                moveBetter(closest(V.rc.getAllySpawnLocations()));
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

    public static void moveBfs(V.BfsTarget target) throws GameActionException {
        switch (target) {
            case CENTRE:
                moveBfsUtil(V.centreBfs, target);
                break;
            case SPAWN:
                moveBfsUtil(V.spawnBfs, target);
                break;
            case FLAG0:
                moveBfsUtil(V.flag0Bfs, target);
                break;
            case FLAG1:
                moveBfsUtil(V.flag1Bfs, target);
                break;
            case FLAG2:
                moveBfsUtil(V.flag2Bfs, target);
                break;
            default:
                System.out.println("Something went wrong in moveBfs()!");
        }
    }

    /**
     * Helper function attempting to buy global upgrades.
     * Priority:
     * 1. <code>GlobalUpgrade.ACTION</code>
     * 2. <code>GlobalUpgrade.HEALING</code>
     * @throws GameActionException
     */
    public static void buyGlobal() throws GameActionException {
        if (V.rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
            V.rc.buyGlobal(GlobalUpgrade.ATTACK);
        }
        if (V.rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
            V.rc.buyGlobal(GlobalUpgrade.HEALING);
        }
    }

    public static void run(RobotController _rc) throws GameActionException {
        V.rc = _rc;
        V.rng = new Random(V.rc.getID());
        MapLocation targetCell = new MapLocation(-1, -1);
        V.width = V.rc.getMapWidth();
        V.height = V.rc.getMapHeight();
        V.widthMinus1 = V.width - 1;
        V.widthMinus2 = V.width - 2;
        V.widthMinus3 = V.width - 3;
        V.heightMinus1 = V.height - 1;
        V.heightMinus2 = V.height - 2;
        V.heightMinus3 = V.height - 3;
        V.centre = new MapLocation(V.width / 2, V.height / 2);
        V.board = new int[V.width][V.height];
        for (MapLocation spawn: V.rc.getAllySpawnLocations()) {
            V.board[spawn.x][spawn.y] = 3;
        }
        while (true) {
            try {
                V.round = V.rc.getRoundNum();
                if (!V.rc.isSpawned()) {
                    MapLocation[] spawnLocs = V.rc.getAllySpawnLocations();
                    // Arrays.sort(spawnLocs, closestComp(centre));
                    for(int i = spawnLocs.length - 1; i > 0; i--) {
                        int j = V.rng.nextInt(i);
                        MapLocation tmp = spawnLocs[i];
                        spawnLocs[i] = spawnLocs[j];
                        spawnLocs[j] = tmp;
                    }
                    for (MapLocation loc : spawnLocs) {
                        if (V.rc.canSpawn(loc)) {
                            V.rc.spawn(loc);
                            swarmTarget = new MapLocation(-1, -1);
                            if (V.round == 1) {
                                shuffle();
                                for (Direction dir : V.shuffledDirections) {
                                    if (V.rc.canMove(dir)) {
                                        if (!V.rc.senseMapInfo(V.rc.getLocation().add(dir)).isSpawnZone()) {
                                            V.rc.move(dir);
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                    if (!V.rc.isSpawned()) {
                        continue;
                    }
                }
                if (V.round == 1) {
                    int i = 0;
                    while (V.rc.readSharedArray(i) > 0) {
                        i++;
                    }
                    V.rc.writeSharedArray(i, V.rc.getID() - 9999);
                } else if (V.round == 2) {
                    for (int i = 0; i < 50; ++i) {
                        int id = V.rc.readSharedArray(i);
                        V.ids[i] = id;
                        V.idx[id] = i;
                        if (id + 9999 == V.rc.getID()) {
                            V.selfIdx = i;
                        }
                    }
                    if (V.selfIdx == 49) {
                        for (int i = 0; i < 50; ++i) {
                            V.rc.writeSharedArray(i, 0);
                        }
                    }
                    int f = 0;
                    for (MapLocation m : V.rc.getAllySpawnLocations()){
                        int adjCount = 0;
                        for(MapLocation m2 : V.rc.getAllySpawnLocations()) {
                            if(Math.abs(m.x - m2.x) <= 1 && Math.abs(m.y - m2.y) <= 1) adjCount++;
                        }
                        if (adjCount == 9){
                            V.spawnCentres[f] = m;
                            f++;
                        }
                    }
                    for(int i = 40; i < 43; i++) {
                        if(V.ids[i] + 9999 == V.rc.getID()) V.home = V.spawnCentres[i - 40];
                    }
                    for(int i = 37; i < 40; i++) {
                        if(V.ids[i] + 9999 == V.rc.getID()) V.isBuilder = true;
                    }
                } else if (V.round >= 3 && V.round <= 160) {
                    if (V.rc.isSpawned()) {
                        if (V.selfIdx >= 45) {
                            for (int i = 4; i < 64; i += 2) {
                                decodeBroadcast(i);
                            }
                        } else {
                            recordVision();
                            if (V.selfIdx >= 2 && V.selfIdx <= 31) {
                                broadcastVision(V.selfIdx << 1);
                            }
                        }
                    }
                } else if (V.round == 161) {
                    if (V.selfIdx <= 44) {
                        // all robots broadcast their interpretation of the map's symmetry
                        broadcastSymmetry(V.selfIdx + 19);
                    } else {
                        // receive symmetry broadcasts from other robots
                        for (int i = 19; i < 64; ++i) {
                            decodeSymmetry(i);
                        }
                        if (V.symmetry == V.Symmetry.VERTICAL) {
                            V.getOpp = (x, y) -> V.board[x][V.heightMinus1 - y];
                        } else if (V.symmetry == V.Symmetry.HORIZONTAL) {
                            V.getOpp = (x, y) -> V.board[V.widthMinus1 - x][y];
                        } else {
                            V.getOpp = (x, y) -> V.board[V.widthMinus1 - x][V.heightMinus1 - y];
                        }
                    }
                    if (V.selfIdx >= 45) {
                        V.q = new StringBuilder();
                        MapLocation loc = new MapLocation(1, 1);  // will use Aiden's centre spawns once pulled
                        V.bfs = new int[V.width][V.height];
                        for (int x = 0; x < V.width; ++x) {
                            UnrolledUtils.fill(V.bfs[x], -1);
                        }
                        if (V.selfIdx == 45) {
                            V.q.append(hashLoc(V.centre));
                            V.bfs[V.centre.x][V.centre.y] = 0;
                        } else if (V.selfIdx == 46) {
                            for (MapLocation spawn: V.rc.getAllySpawnLocations()) {
                                V.q.append(hashLoc(spawn));
                                V.bfs[spawn.x][spawn.y] = 0;
                            }
                        } else if (V.selfIdx == 47) {
                            V.q.append(hashLoc(loc));
                        } else if (V.selfIdx == 48) {
                            V.q.append(hashLoc(loc));
                        } else if (V.selfIdx == 49) {
                            V.q.append(hashLoc(loc));
                        }
                        V.optimal = new int[V.width][V.height];
                        V.rc.writeSharedArray(V.selfIdx + 14, 0);
                        continue;
                    }
                } else if (V.round >= 162 && V.bfsIdx < 50) {
                    if (V.selfIdx >= 45 && !V.bfsDone) {
                        // BFS time baby!
                        while (V.q.length() > 0) {
                            int x1 = V.q.charAt(0) / V.height, y1 = V.q.charAt(0) % V.height;
                            V.q.deleteCharAt(0);
                            int nextDist = V.bfs[x1][y1] + 1;
                            for (int i = 0; i < 8; ++i) {
                                int x2 = x1 + V.directions[i].opposite().getDeltaX(), y2 = y1 + V.directions[i].opposite().getDeltaY();
                                if (x2 >= 0 && x2 < V.width && y2 >= 0 && y2 < V.height) {
                                    if ((V.board[x2][y2] | V.getOpp.applyAsInt(x2, y2)) == 0 && V.bfs[x2][y2] == -1) {
                                        V.bfs[x2][y2] = nextDist;
                                        V.optimal[x2][y2] = i + 1;
                                        V.q.append((char) (x2 * V.height + y2));
                                    }
                                }
                            }
                            if (Clock.getBytecodesLeft() < 1000) {
                                break;
                            }
                        }
                        if (V.q.length() > 0) {
                            continue;
                        } else {
                            V.bfsDone = true;
                            V.rc.writeSharedArray(V.selfIdx + 14, 1);
                            V.bfsIdx = -1;
                        }
                        continue;
                    }
                    if (V.bfsIdx == -2) {
                        V.bfsIdx = -1;
                    } else if (V.bfsIdx == -1) {
                        V.bfsIdx = 45;
                        for (int i = 59; i < 64; ++i) {
                            if (V.rc.readSharedArray(i) == 0) {
                                V.bfsIdx = -1;
                                break;
                            }
                        }
                        if (V.bfsIdx != -1) {
                            System.out.println(V.selfIdx + ", ready!");
                            if (V.bfsIdx == V.selfIdx) {
                                broadcastBfs();
                            } else if (V.bfsIdx < V.selfIdx) {
                                receiveBfs();
                            }
                        }
                    } else if (V.bfsIdx == V.selfIdx) {
                        broadcastBfs();
                    } else if (V.bfsIdx == -3) {
                        V.bfsIdx = V.selfIdx + 1;
                    } else {
                        receiveBfs();
                    }
                }
                buyGlobal();
                if(V.rc.onTheMap(V.home)) {
                    if(!V.rc.getLocation().equals(V.home)) {
                        moveBetter(V.home);
                        V.rc.setIndicatorLine(V.rc.getLocation(), V.home, 0, 0, 255);
                    }
                    if(V.rc.getLocation().equals(V.home)) trapSpawn();
                } else if (V.round <= 150) {
                    if(V.isBuilder) {
                        farmBuildXp(6);
                        farmBuildXp(6);
                        farmBuildXp(6);
                        farmBuildXp(6);
                    }
                    V.nearbyAllies = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
                    MapLocation nextLoc;
                    MapLocation[] rawCrumbs = V.rc.senseNearbyCrumbs(-1);
                    List<MapLocation> validCrumbs = new ArrayList<MapLocation>();
                    for (MapLocation crumb : rawCrumbs) {
                        if (!V.rc.senseMapInfo(crumb).isWall()) validCrumbs.add(crumb);
                    }
                    MapLocation[] crumbs = validCrumbs.toArray(new MapLocation[0]);
                    if(crumbs.length > 0) {
                        // sort crumbs by distance
                        Arrays.sort(crumbs, closestComp());
                        nextLoc = crumbs[0];
                    } else {
                        if(V.movesLeft > 0 && !V.rc.getLocation().equals(V.target)) {
                            V.movesLeft--;
                        } else {
                            V.target = new MapLocation(StrictMath.max(0, StrictMath.min(V.width - 1, V.rc.getLocation().x + V.rng.nextInt(21) - 10)),
                                    StrictMath.max(0, StrictMath.min(V.height - 1, V.rc.getLocation().y + V.rng.nextInt(21) - 10)));
                            V.movesLeft = 7;
                        }
                        nextLoc = V.target;
                    }
                    moveBetter(nextLoc);
                    continue;
                }
                FlagInfo[] oppFlags = V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent());
                RobotInfo[] friends = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
                RobotInfo[] enemies = V.rc.senseNearbyRobots(9, V.rc.getTeam().opponent());
                MapLocation[] enemyLocs = new MapLocation[enemies.length];
                for (int i = 0; i < enemies.length; ++i) enemyLocs[i] = enemies[i].getLocation();
                // if (oppFlags.length > 0) {
                //     if (V.rc.hasFlag()) {
                //         if (enemies.length > 0 && false) {
                //             V.rc.move(V.rc.getLocation().directionTo(closest(enemyLocs)).opposite());
                //         } else {
                //             moveBfs(BfsTarget.SPAWN);
                //         }
                //         continue;
                //     }
                //     UnrolledUtils.shuffle(oppFlags, rng);
                //     for (FlagInfo flag: oppFlags) {
                //         if (!flag.isPickedUp()) {
                //             MapLocation loc = flag.getLocation();
                //             MapLocation[] next = {};
                //             if (spawnBfs != null) {
                //                 int i = spawnBfs[loc.x][loc.y];
                //                 if (i > 0) {
                //                     Direction opt = directions[i - 1];
                //                     next = new MapLocation[]{loc.add(opt), loc.add(opt.rotateLeft()), loc.add(opt.rotateRight())};
                //                 }
                //             }

                //             // first, see if we're in an optimal position to pick it up
                //             boolean canPickup = V.rc.canPickupFlag(loc);
                //             if (canPickup) {
                //                 for (MapLocation nextLoc : next) {
                //                     if (V.rc.getLocation().equals(nextLoc)) {
                //                         V.rc.pickupFlag(loc);
                //                         break;
                //                     }
                //                 }
                //             }

                //             if (!V.rc.hasFlag()) {
                //                 // second, check if another friend with HIGHER ID is in an optimal position to pick it up
                //                 boolean canFriendPickup = true;
                //                 for (MapLocation nextLoc : next) {
                //                     if (V.rc.onTheMap(nextLoc) && V.rc.canSenseLocation(nextLoc)) {
                //                         RobotInfo friend = V.rc.senseRobotAtLocation(nextLoc);
                //                         if (friend != null && friend.getTeam() == V.rc.getTeam() && idx[V.rc.getID() - 9999] > selfIdx) {
                //                             canFriendPickup = false;
                //                             break;
                //                         }
                //                     }
                //                 }

                //                 if (!canFriendPickup) {
                //                     // then, try move to an optimal position to pick it up
                //                     for (MapLocation nextLoc: next) {
                //                         if (V.rc.onTheMap(nextLoc) && V.rc.canSenseLocation(nextLoc)) {
                //                             Direction dir = V.rc.getLocation().directionTo(nextLoc);
                //                             if (V.rc.adjacentLocation(dir).equals(nextLoc) && V.rc.canMove(dir)) {
                //                                 V.rc.move(dir);
                //                                 break;
                //                             }
                //                         }
                //                     }
                //                     // if unable to move to an optimal position, don't bother
                //                     // at such short distances, BugNav might produce unexpected behaviours

                //                     // pickup the flag, at last
                //                     V.rc.pickupFlag(loc);
                //                 }
                //             }
                //         }
                //     }
                //     lastFlag = (V.rc.senseBroadcastFlagLocations().length + oppFlags.length) <= 1 || round >= 1750;
                //     if (lastFlag) debug("LAST FLAG");
                //     for (FlagInfo flag: V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent())) {
                //         if (flag.isPickedUp() && lastFlag) {
                //             debug("LET'S SWARM");
                //             MapLocation loc = flag.getLocation();
                //             moveBetter(loc);
                //             if (V.rc.canSenseLocation(loc) && V.rc.canHeal(loc)) {
                //                 V.rc.heal(loc);
                //             }
                //         }
                //     }
                // }
                V.nearbyAllies = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
                pickupFlag(true);
                // Find all triggered stun traps;
                updateStuns();
                if(V.isBuilder) {
                    buildTraps();
                    farmBuildXp(4);
                    farmBuildXp(4);
                    farmBuildXp(4);
                    farmBuildXp(4);
                }
                if(V.rc.senseNearbyFlags(0).length == 0) {
                    healFlagBearer();
                    attack();
                }
                targetCell = findTarget();
                // Determine whether to move or not
                int nearbyHP = V.rc.getHealth();
                for (RobotInfo ally : V.nearbyAllies) {
                    nearbyHP += ally.health;
                }
                int threshold = StrictMath.min(V.nearbyAllies.length * 75, 751) * (V.nearbyAllies.length + 1);
                int enemyHP = 0;
                RobotInfo[] rawEnemies = V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent());
                List<RobotInfo> listEnemies = new ArrayList<RobotInfo>();
                // sittingDucks contains all stunned enemies
                List<RobotInfo> listSittingDucks = new ArrayList<RobotInfo>();
                for(RobotInfo enemy : rawEnemies) {
                    boolean skip = false;
                    for (ActiveStun stun : V.activeStuns) {
                        if (enemy.getLocation().distanceSquaredTo(stun.location) <= 13) {
                            skip = true;
                            break;
                        }
                    }
                    if (skip) {
                        listSittingDucks.add(enemy);
                        V.rc.setIndicatorDot(enemy.getLocation(), 255, 255, 0);
                        continue;
                    }
                    enemyHP += enemy.health;
                    listEnemies.add(enemy);
                }
                enemies = listEnemies.toArray(new RobotInfo[0]);
                V.sittingDucks = listSittingDucks.toArray(new RobotInfo[0]);
                // Movement
                {
                    if (V.sittingDucks.length > 0 && !V.rc.hasFlag() && !V.isBuilder) {
                        int minDistSquared = 100;
                        MapLocation[] choices = new MapLocation[V.sittingDucks.length];
                        int n = 0;
                        for (RobotInfo enemy : V.sittingDucks) {
                            int distToEnemy = enemy.getLocation().distanceSquaredTo(V.rc.getLocation());
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
                        V.rc.setIndicatorLine(V.rc.getLocation(), bestChoice, 255, 0, 0);
                        //System.out.println("Moving towards a sitting duck");
                        moveBetter(bestChoice);
                    } else if (enemyHP * 5 > nearbyHP * 2
                               && !V.rc.hasFlag()
                               && V.rc.senseNearbyFlags(9, V.rc.getTeam().opponent()).length == 0
                               && V.rc.senseNearbyFlags(4, V.rc.getTeam()).length == 0) {
                        // Begin Kiting
                        int currTargeted = 0;
                        Direction[] choices = new Direction[8];
                        MapLocation rcLocation = V.rc.getLocation();
                        boolean returnToCombat = V.rc.isActionReady() && V.rc.getHealth() > 750; // Only return if currently safe
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
                        for (Direction dir : V.shuffledDirections) {
                            if (V.rc.canMove(dir)) {
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
                            if (V.rc.getHealth() < 900 && enemies.length > 0) {
                                debug("RETREAT");
                                RobotInfo nearestEnemy = enemies[0];
                                int nearestEnemyDistSquared = 1000;
                                for (RobotInfo enemy : enemies) {
                                    int currDistSquared = enemy.getLocation().distanceSquaredTo(V.rc.getLocation());
                                    if (currDistSquared < nearestEnemyDistSquared) {
                                        nearestEnemyDistSquared = currDistSquared;
                                        nearestEnemy = enemy;
                                    }
                                }
                                final RobotInfo actualNearestEnemy = nearestEnemy;
                                Arrays.sort(choices, 0, n, (a, b) -> {
                                    return V.rc.getLocation().add(b).distanceSquaredTo(actualNearestEnemy.getLocation()) - V.rc.getLocation().add(a).distanceSquaredTo(actualNearestEnemy.getLocation());
                                });
                            } else {
                                Arrays.sort(choices, 0, n, (a, b) -> {
                                    return V.rc.getLocation().add(a).distanceSquaredTo(finalTargetCell) - V.rc.getLocation().add(b).distanceSquaredTo(finalTargetCell);
                                });
                            }
                            V.rc.move(choices[0]);
                        } else {
                            // There are no choices
                            shuffle();
                            int minAdj = V.rc.senseNearbyRobots(2, V.rc.getTeam()).length;
                            Direction choice = Direction.NORTH;
                            boolean overridden = false;
                            for (Direction dir : V.shuffledDirections) {
                                if (V.rc.canMove(dir)) {
                                    int adjCount = V.rc.senseNearbyRobots(-1, V.rc.getTeam()).length;
                                    if (adjCount < minAdj) {
                                        choice = dir;
                                        minAdj = adjCount;
                                        overridden = true;
                                    }
                                }
                            }
                            if (overridden) {
                                V.rc.move(choice);
                            } else if (returnToCombat) {
                                // Attempt to move towards enemies
                                if (V.sittingDucks.length > 0) {
                                    V.rc.setIndicatorLine(V.rc.getLocation(), V.sittingDucks[0].getLocation(), 255, 0, 0);
                                    //System.out.println("Moving towards a sitting duck");
                                    moveBetter(V.sittingDucks[0].getLocation());
                                } else {
                                    moveBetter(enemies[0].getLocation());
                                }
                            }
                        }
                    } else if (nearbyHP >= threshold || V.rc.senseNearbyFlags(13, V.rc.getTeam().opponent()).length == 0) {
                        moveBetter(targetCell);
                        debug(targetCell);
                        debug("Nope, not kiting");
                    }
                }
                pickupFlag(true);
                healFlagBearer();
                attack();
                if (V.round > 1900) farmBuildXp(3);
                heal();
                if(V.rc.getCrumbs() > 5000) buildTraps();
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                if (V.rc.onTheMap(targetCell) && V.rc.isSpawned()) {
                    V.rc.setIndicatorLine(V.rc.getLocation(), targetCell, 0, 255, 0);
                }
                debug("TD: " + String.valueOf(V.turnDir));
                debug("SS: " + String.valueOf(V.stackSize));
                V.rc.setIndicatorString(V.indicatorString);
                V.indicatorString = "";
                Clock.yield();
            }
        }
    }
}
