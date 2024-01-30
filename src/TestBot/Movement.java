package TestBot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

public class Movement {
    private static MapLocation[] enemySpawns;

    public static boolean move(Direction dir) {
        try {
            V.rc.move(dir);
            RobotUtils.updateRobots();
            return true;
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
        return false;
    }

    public static boolean moveSafe(Direction dir) {
        if (V.rc.canMove(dir)) {
            return move(dir);
        }
        return false;
    }

    public static void AllMovements() throws GameActionException {
//        if (V.allies.length < V.enemies.length * 3) {
        Building.updateStuns();
        V.targetCell = Targetting.findTarget();
        V.micro.doMicro();
//        }
//        RobotInfo[] enemies = V.rc.senseNearbyRobots(9, V.rc.getTeam().opponent());
//        V.nearbyAllies = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
//        // Find all triggered stun traps;
//        Building.updateStuns();
//        V.targetCell = Targetting.findTarget();
//        // Determine whether to move or not
//        int nearbyHP = V.rc.getHealth();
//        for (RobotInfo ally : V.nearbyAllies) {
//            nearbyHP += ally.health;
//        }
//        int threshold = StrictMath.min(V.nearbyAllies.length * 75, 751) * (V.nearbyAllies.length + 1);
//        int enemyHP = 0;
//        RobotInfo[] rawEnemies = V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent());
//        List<RobotInfo> listEnemies = new ArrayList<RobotInfo>();
//        // sittingDucks contains all stunned enemies
//        List<RobotInfo> listSittingDucks = new ArrayList<RobotInfo>();
//        // occupiedEnemies contains all enemies that are already attacking
//        List<RobotInfo> listOccupiedEnemies = new ArrayList<RobotInfo>();
//        for(RobotInfo enemy : rawEnemies) {
//            boolean skip = false;
//            for (ActiveStun stun : V.activeStuns) {
//                if (enemy.getLocation().distanceSquaredTo(stun.location) <= 13) {
//                    skip = true;
//                    break;
//                }
//            }
//            if (skip) {
//                listSittingDucks.add(enemy);
//                V.rc.setIndicatorDot(enemy.getLocation(), 255, 255, 0);
//                continue;
//            }
//            enemyHP += enemy.health;
//            boolean occupied = false;
//            RobotInfo[] alliesAroundEnemy = V.rc.senseNearbyRobots(enemy.getLocation(), 4, V.rc.getTeam());
//            for (RobotInfo ally : alliesAroundEnemy) {
//                if (V.idx[ally.getID()-10000] < V.selfIdx) {
//                    occupied = true;
//                    break;
//                }
//            }
//            if (occupied) {
//                listOccupiedEnemies.add(enemy);
//            } else {
//                listEnemies.add(enemy);
//            }
//        }
//        enemies = listEnemies.toArray(new RobotInfo[0]);
//        V.sittingDucks = listSittingDucks.toArray(new RobotInfo[0]);
//        RobotInfo[] occupiedEnemies = listOccupiedEnemies.toArray(new RobotInfo[0]);
//        // Movement
//        {
//            if (V.sittingDucks.length > 0 && !V.rc.hasFlag() && !V.isBuilder) {
//                int minDistSquared = 100;
//                MapLocation[] choices = new MapLocation[V.sittingDucks.length];
//                int n = 0;
//                for (RobotInfo enemy : V.sittingDucks) {
//                    int distToEnemy = enemy.getLocation().distanceSquaredTo(V.rc.getLocation());
//                    if (distToEnemy < minDistSquared) {
//                        minDistSquared = distToEnemy;
//                        n = 0;
//                    }
//                    if (distToEnemy == minDistSquared) {
//                        choices[n++] = enemy.getLocation();
//                    }
//                }
//                MapLocation bestChoice = choices[0];
//                if (V.targetCell != null) {
//                    int shortestDist = -1;
//                    for (int i = 0; i < n; ++i) {
//                        MapLocation choice = choices[i];
//                        int distToTarget = choice.distanceSquaredTo(V.targetCell);
//                        if (shortestDist == -1 || distToTarget < shortestDist) {
//                            shortestDist = distToTarget;
//                            bestChoice = choice;
//                        }
//                    }
//                }
//                V.rc.setIndicatorLine(V.rc.getLocation(), bestChoice, 255, 0, 0);
//                //System.out.println("Moving towards a sitting duck");
//                BugNav.moveBetter(bestChoice);
//            } else if (enemyHP * 5 > nearbyHP * 2
//                    && !V.rc.hasFlag()
//                    && V.rc.senseNearbyFlags(9, V.rc.getTeam().opponent()).length == 0
//                    && V.rc.senseNearbyFlags(4, V.rc.getTeam()).length == 0) {
//                // Begin Kiting
//                int currTargeted = 0;
//                Direction[] choices = new Direction[8];
//                MapLocation rcLocation = V.rc.getLocation();
//                boolean returnToCombat = V.rc.isActionReady() && V.rc.getHealth() > 750; // Only return if currently safe
//                for (RobotInfo enemy : enemies) {
//                    if (enemy.getLocation().isWithinDistanceSquared(rcLocation, 4)) {
//                        currTargeted++;
//                        returnToCombat = false;
//                    }
//                }
//                int minTargeted = currTargeted;
//                if (returnToCombat) {
//                    minTargeted = 1000; // Reset min
//                }
//                int n = 0; // size of choices;
//                RobotUtils.shuffle(V.shuffledDirections);
//                for (Direction dir : V.shuffledDirections) {
//                    if (V.rc.canMove(dir)) {
//                        int count = 0;
//                        for (RobotInfo enemy : enemies) {
//                            if (enemy.getLocation().isWithinDistanceSquared(rcLocation.add(dir), 4)) {
//                                count++;
//                            }
//                        }
//                        if (!(returnToCombat && count == 0)) {
//                            if (count < minTargeted) {
//                                minTargeted = count;
//                                n = 0;
//                            }
//                            if (count == minTargeted) {
//                                choices[n++] = dir;
//                            }
//                        }
//                    }
//                }
//                if (n > 0) {
//                    // Choose the best choice
//                    // Choices are either the safest, or safest where we can attack
//                    MapLocation finaltargetCell = V.targetCell;
//                    if (V.rc.getHealth() < 900 && enemies.length > 0) {
//                        RobotUtils.debug("RETREAT");
//                        RobotInfo nearestEnemy = enemies[0];
//                        int nearestEnemyDistSquared = 1000;
//                        for (RobotInfo enemy : enemies) {
//                            int currDistSquared = enemy.getLocation().distanceSquaredTo(V.rc.getLocation());
//                            if (currDistSquared < nearestEnemyDistSquared) {
//                                nearestEnemyDistSquared = currDistSquared;
//                                nearestEnemy = enemy;
//                            }
//                        }
//                        final RobotInfo actualNearestEnemy = nearestEnemy;
//                        Arrays.sort(choices, 0, n, (a, b) -> {
//                            return V.rc.getLocation().add(b).distanceSquaredTo(actualNearestEnemy.getLocation()) - V.rc.getLocation().add(a).distanceSquaredTo(actualNearestEnemy.getLocation());
//                        });
//                    } else {
//                        Arrays.sort(choices, 0, n, (a, b) -> {
//                            return V.rc.getLocation().add(a).distanceSquaredTo(finaltargetCell) - V.rc.getLocation().add(b).distanceSquaredTo(finaltargetCell);
//                        });
//                    }
//                    if (V.rc.canMove(choices[0])) V.rc.move(choices[0]);
//                } else {
//                    // There are no choices
//                    RobotUtils.shuffle(V.shuffledDirections);
//                    int minAdj = V.rc.senseNearbyRobots(2, V.rc.getTeam()).length;
//                    Direction choice = Direction.NORTH;
//                    boolean overridden = false;
//                    for (Direction dir : V.shuffledDirections) {
//                        if (V.rc.canMove(dir)) {
//                            int adjCount = V.rc.senseNearbyRobots(-1, V.rc.getTeam()).length;
//                            if (adjCount < minAdj) {
//                                choice = dir;
//                                minAdj = adjCount;
//                                overridden = true;
//                            }
//                        }
//                    }
//                    if (overridden) {
//                        V.rc.move(choice);
//                    } else if (returnToCombat) {
//                        // Attempt to move towards enemies
//                        if (V.sittingDucks.length > 0) {
//                            V.rc.setIndicatorLine(V.rc.getLocation(), V.sittingDucks[0].getLocation(), 255, 0, 0);
//                            //System.out.println("Moving towards a sitting duck");
//                            BugNav.moveBetter(V.sittingDucks[0].getLocation());
//                        } else if (enemies.length > 0) {
//                            BugNav.moveBetter(enemies[0].getLocation());
//                        } else {
//                            BugNav.moveBetter(occupiedEnemies[0].getLocation());
//                        }
//                    }
//                }
//            } else if (nearbyHP >= threshold || V.rc.senseNearbyFlags(13, V.rc.getTeam().opponent()).length == 0) {
//                BugNav.moveBetter(V.targetCell);
//                RobotUtils.debug(V.targetCell);
////                RobotUtils.debug("Nope, not kiting");
//            }
//        }
    }
    public static void SetupFlags() throws GameActionException {
        if (!V.rc.hasFlag() && !V.setFlag) {
            // Move towards the flag
            // We don't have it and the flag is not set, go to the flagHome
            if (!V.rc.getLocation().equals(V.flagHome)) {
                BugNav.moveBetter(V.flagHome);
                V.rc.setIndicatorLine(V.rc.getLocation(), V.flagHome, 0, 0, 255);
            }
            FlagInfo[] nearbyFlags = V.rc.senseNearbyFlags(2, V.rc.getTeam());
            if (nearbyFlags.length > 0 && V.rc.canPickupFlag(nearbyFlags[0].getLocation())) {
                V.rc.pickupFlag(nearbyFlags[0].getLocation());
            }
        }
        if (V.rc.hasFlag()) {
            if (V.round < 200) {
                BFSFlagMove();
            } else {
                GreedyFlagMove();
            }
        }
    }
    private static int weighting(MapLocation loc) throws GameActionException {
        int res = V.flagWeights[loc.x][loc.y];
        if (V.flagWeights[loc.x][loc.y] == -100) {
            int bfsDist = V.flagBfs.dist(loc);
            if (bfsDist == 10000) return -1;
            int wallWeight = 0;
            { // This is a loop.
                // The reasoning for using 85 is that the max dist across the map is around 84.85 smth
                //wallWeight = UnrolledWeightingHelper(loc);
            }
            V.flagWeights[loc.x][loc.y] = bfsDist+wallWeight;
            return bfsDist+wallWeight;
        }
        return res;
    }
    private static void BFSFlagMove() throws GameActionException {
        // Move the flag to a safe spot
        RobotUtils.debug("bfsFlag");
        List<MapLocation> listEnemySpawns = new ArrayList<MapLocation>();
        /*for (MapLocation centre : V.spawnCentres) {
            if (V.horizontal) {
                listEnemySpawns.add(new MapLocation(V.rc.getMapWidth()-1-centre.x, centre.y));
                V.rc.setIndicatorDot(listEnemySpawns.get(listEnemySpawns.size()-1), 255, 0, 255);
            }
            if (V.vertical) {
                listEnemySpawns.add(new MapLocation(centre.x, V.rc.getMapHeight()-1-centre.y));
                V.rc.setIndicatorDot(listEnemySpawns.get(listEnemySpawns.size()-1), 255, 255, 0);
            }
            if (V.rotational) {
                listEnemySpawns.add(new MapLocation(V.rc.getMapWidth()-1-centre.x, V.rc.getMapHeight()-1-centre.y));
                V.rc.setIndicatorDot(listEnemySpawns.get(listEnemySpawns.size()-1), 0, 255, 255);
            }
        }*/
        //RobotUtils.debug("V:"+V.vertical+",H:"+V.horizontal+",R:"+V.rotational);
        //enemySpawns = listEnemySpawns.toArray(new MapLocation[0]);
        //RobotUtils.debug("V:"+V.vertical+",H:"+V.horizontal+",R:"+V.rotational);
        //RobotUtils.shuffle(V.shuffledDirections);
        int bestDist = weighting(V.rc.getLocation());
        MapLocation bestLoc = null;
        //List<MapLocation> bestLocs = new ArrayList<MapLocation>();
        MapLocation[] bestLocs = new MapLocation[64];
        int n = 0;
        int maxDist = StrictMath.max(V.rc.getMapWidth(), V.rc.getMapHeight());
        MapInfo[] vicinity = V.rc.senseNearbyMapInfos((V.round < 200-2*(maxDist/3))?20:2);
        RobotUtils.shuffle(vicinity);
        for (MapInfo mi : vicinity) {
            if (!mi.isPassable()) continue;
            int distToEnemy = weighting(mi.getMapLocation());
            if (V.rc.sensePassability(mi.getMapLocation())
                    && RobotUtils.validFlagPlacement(mi.getMapLocation())) {
                if (distToEnemy > bestDist) {
                    n = 0;
                    bestDist = distToEnemy;
                }
                if (distToEnemy == bestDist) {
                    bestLocs[n++] = mi.getMapLocation();
                }
            }
            // This is debug to show the weightings of processed maplocations
            int colour = (int)Math.max(Math.min(((distToEnemy-30)/40d)*255d, 255d), 0d);
            V.rc.setIndicatorDot(mi.getMapLocation(), colour, colour, colour);
        }
        if (n > 0) {
            bestLoc = bestLocs[0];
            bestDist = V.home.distanceSquaredTo(bestLoc);
            for (int i = 0; i < n; ++i) {
                MapLocation loc = bestLocs[i];
                int currDist = V.home.distanceSquaredTo(loc);
                if (currDist < bestDist) {
                    bestDist = currDist;
                    bestLoc = loc;
                }
            }
        }
        if (bestLoc == null || bestLoc.equals(V.rc.getLocation())) {
            // Stay still I guess
            //if (V.rc.canDropFlag(V.rc.getLocation())) V.rc.dropFlag(V.rc.getLocation());
            //V.setFlag = true;
            if (!RobotUtils.validFlagPlacement(V.rc.getLocation())) {
                // Attempt to get out of invalid zone
                Direction attempt = RobotUtils.moveOut(V.rc.getLocation());
                BugNav.moveBetter(V.rc.adjacentLocation(attempt));
            }
        } else {
            BugNav.moveBetter(bestLoc);
            //if (V.rc.canDropFlag(V.rc.getLocation())) V.rc.dropFlag(V.rc.getLocation());
            V.rc.setIndicatorLine(V.rc.getLocation(),bestLoc, 255, 0, 255);
            V.flagHome = V.rc.getLocation();
        }
        V.rc.writeSharedArray(V.selfIdx-Consts.LOWEST_FLAG_SITTER+Consts.LOWEST_FS_COMMS_IDX, Comms.encode(V.flagHome));
    }
    private static void GreedyFlagMove() throws GameActionException{/*
        // Move the flag to a safe spot
        RobotUtils.debug("greedyFlag");
        List<MapLocation> listEnemySpawns = new ArrayList<MapLocation>();
        for (MapLocation centre : V.spawnCentres) {
            if (V.horizontal) {
                listEnemySpawns.add(new MapLocation(V.rc.getMapWidth()-1-centre.x, centre.y));
                V.rc.setIndicatorDot(listEnemySpawns.get(listEnemySpawns.size()-1), 255, 0, 255);
            }
            if (V.vertical) {
                listEnemySpawns.add(new MapLocation(centre.x, V.rc.getMapHeight()-1-centre.y));
                V.rc.setIndicatorDot(listEnemySpawns.get(listEnemySpawns.size()-1), 255, 255, 0);
            }
            if (V.rotational) {
                listEnemySpawns.add(new MapLocation(V.rc.getMapWidth()-1-centre.x, V.rc.getMapHeight()-1-centre.y));
                V.rc.setIndicatorDot(listEnemySpawns.get(listEnemySpawns.size()-1), 0, 255, 255);
            }
        }
        //RobotUtils.debug("V:"+V.vertical+",H:"+V.horizontal+",R:"+V.rotational);
        MapLocation[] enemySpawns = listEnemySpawns.toArray(new MapLocation[0]);
        RobotUtils.shuffle(V.shuffledDirections);
        int bestDist = RobotUtils.closest(enemySpawns).distanceSquaredTo(V.rc.getLocation());
        MapLocation bestLoc = null;
        MapInfo[] vicinity = V.rc.senseNearbyMapInfos((V.round < 140)?20:2);
        RobotUtils.shuffle(vicinity);
        for (MapInfo mi : vicinity) {
            if (!mi.isPassable() || mi.getMapLocation() == V.rc.getLocation()) continue;
            int distToEnemy = RobotUtils.closest(enemySpawns, mi.getMapLocation()).distanceSquaredTo(mi.getMapLocation());
            if (V.rc.sensePassability(mi.getMapLocation())
                    && RobotUtils.validFlagPlacement(mi.getMapLocation())
                    && distToEnemy > bestDist) {
                bestLoc = mi.getMapLocation();
                bestDist = distToEnemy;
            }
        }
        if (bestLoc == null) {
            // Stay still I guess
            if (V.rc.canDropFlag(V.rc.getLocation())) V.rc.dropFlag(V.rc.getLocation());
            //V.setFlag = true;
        } else {
            BugNav.moveBetter(bestLoc);
            //if (V.rc.canDropFlag(V.rc.getLocation())) V.rc.dropFlag(V.rc.getLocation());
            V.rc.setIndicatorLine(V.rc.getLocation(),bestLoc, 255, 0, 255);
            V.flagHome = V.rc.getLocation();
            V.rc.writeSharedArray(V.selfIdx-Consts.LOWEST_FLAG_SITTER+Consts.LOWEST_FS_COMMS_IDX, Comms.encode(V.flagHome));
        }
    */}
    private static int UnrolledWeightingHelper(MapLocation loc) {
        int x = loc.x, y = loc.y;
        int wallWeight = 0;
        int curr = loc.distanceSquaredTo(RobotUtils.closest(enemySpawns));
        for (int offx = -3; offx <= 3; ++offx) {
            for (int offy = -3; offy <= 3; ++offy) {
                if (V.rc.onTheMap(new MapLocation(x+offx, y+offy))) {
                    if (V.wallWeights[x+offx][y+offy] == 0) {
                        V.wallWeights[x+offx][y+offy] = (new MapLocation(x+offx, y+offy)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                    }
                    wallWeight += ((curr >= V.wallWeights[x+offx][y+offy])?1:0);
                }
            }
        }
        /* The unrolled loop
        {
            if (V.rc.onTheMap(new MapLocation(x + -2, y + -2))) {
                if (V.wallWeights[x + -2][y + -2] == 0) {
                    V.wallWeights[x + -2][y + -2] = (new MapLocation(x + -2, y + -2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -2][y + -2]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + -2, y + -1))) {
                if (V.wallWeights[x + -2][y + -1] == 0) {
                    V.wallWeights[x + -2][y + -1] = (new MapLocation(x + -2, y + -1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -2][y + -1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + -2, y + 0))) {
                if (V.wallWeights[x + -2][y + 0] == 0) {
                    V.wallWeights[x + -2][y + 0] = (new MapLocation(x + -2, y + 0)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -2][y + 0]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + -2, y + 1))) {
                if (V.wallWeights[x + -2][y + 1] == 0) {
                    V.wallWeights[x + -2][y + 1] = (new MapLocation(x + -2, y + 1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -2][y + 1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + -2, y + 2))) {
                if (V.wallWeights[x + -2][y + 2] == 0) {
                    V.wallWeights[x + -2][y + 2] = (new MapLocation(x + -2, y + 2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -2][y + 2]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + -1, y + -2))) {
                if (V.wallWeights[x + -1][y + -2] == 0) {
                    V.wallWeights[x + -1][y + -2] = (new MapLocation(x + -1, y + -2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -1][y + -2]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + -1, y + -1))) {
                if (V.wallWeights[x + -1][y + -1] == 0) {
                    V.wallWeights[x + -1][y + -1] = (new MapLocation(x + -1, y + -1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -1][y + -1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + -1, y + 0))) {
                if (V.wallWeights[x + -1][y + 0] == 0) {
                    V.wallWeights[x + -1][y + 0] = (new MapLocation(x + -1, y + 0)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -1][y + 0]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + -1, y + 1))) {
                if (V.wallWeights[x + -1][y + 1] == 0) {
                    V.wallWeights[x + -1][y + 1] = (new MapLocation(x + -1, y + 1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -1][y + 1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + -1, y + 2))) {
                if (V.wallWeights[x + -1][y + 2] == 0) {
                    V.wallWeights[x + -1][y + 2] = (new MapLocation(x + -1, y + 2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + -1][y + 2]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 0, y + -2))) {
                if (V.wallWeights[x + 0][y + -2] == 0) {
                    V.wallWeights[x + 0][y + -2] = (new MapLocation(x + 0, y + -2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 0][y + -2]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 0, y + -1))) {
                if (V.wallWeights[x + 0][y + -1] == 0) {
                    V.wallWeights[x + 0][y + -1] = (new MapLocation(x + 0, y + -1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 0][y + -1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 0, y + 0))) {
                if (V.wallWeights[x + 0][y + 0] == 0) {
                    V.wallWeights[x + 0][y + 0] = (new MapLocation(x + 0, y + 0)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 0][y + 0]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 0, y + 1))) {
                if (V.wallWeights[x + 0][y + 1] == 0) {
                    V.wallWeights[x + 0][y + 1] = (new MapLocation(x + 0, y + 1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 0][y + 1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 0, y + 2))) {
                if (V.wallWeights[x + 0][y + 2] == 0) {
                    V.wallWeights[x + 0][y + 2] = (new MapLocation(x + 0, y + 2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 0][y + 2]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 1, y + -2))) {
                if (V.wallWeights[x + 1][y + -2] == 0) {
                    V.wallWeights[x + 1][y + -2] = (new MapLocation(x + 1, y + -2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 1][y + -2]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 1, y + -1))) {
                if (V.wallWeights[x + 1][y + -1] == 0) {
                    V.wallWeights[x + 1][y + -1] = (new MapLocation(x + 1, y + -1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 1][y + -1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 1, y + 0))) {
                if (V.wallWeights[x + 1][y + 0] == 0) {
                    V.wallWeights[x + 1][y + 0] = (new MapLocation(x + 1, y + 0)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 1][y + 0]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 1, y + 1))) {
                if (V.wallWeights[x + 1][y + 1] == 0) {
                    V.wallWeights[x + 1][y + 1] = (new MapLocation(x + 1, y + 1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 1][y + 1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 1, y + 2))) {
                if (V.wallWeights[x + 1][y + 2] == 0) {
                    V.wallWeights[x + 1][y + 2] = (new MapLocation(x + 1, y + 2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 1][y + 2]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 2, y + -2))) {
                if (V.wallWeights[x + 2][y + -2] == 0) {
                    V.wallWeights[x + 2][y + -2] = (new MapLocation(x + 2, y + -2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 2][y + -2]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 2, y + -1))) {
                if (V.wallWeights[x + 2][y + -1] == 0) {
                    V.wallWeights[x + 2][y + -1] = (new MapLocation(x + 2, y + -1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 2][y + -1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 2, y + 0))) {
                if (V.wallWeights[x + 2][y + 0] == 0) {
                    V.wallWeights[x + 2][y + 0] = (new MapLocation(x + 2, y + 0)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 2][y + 0]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 2, y + 1))) {
                if (V.wallWeights[x + 2][y + 1] == 0) {
                    V.wallWeights[x + 2][y + 1] = (new MapLocation(x + 2, y + 1)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 2][y + 1]) ? 1 : 0);
            }
            if (V.rc.onTheMap(new MapLocation(x + 2, y + 2))) {
                if (V.wallWeights[x + 2][y + 2] == 0) {
                    V.wallWeights[x + 2][y + 2] = (new MapLocation(x + 2, y + 2)).distanceSquaredTo(RobotUtils.closest(enemySpawns));
                }
                wallWeight += ((curr >= V.wallWeights[x + 2][y + 2]) ? 1 : 0);
            }
        }
        */
        return wallWeight;
    }
}
