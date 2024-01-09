package RushBot;

import battlecode.common.*;

import java.util.*;

public strictfp class RobotPlayer {
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
    public static void run(RobotController rc) throws GameActionException {
        rng = new Random(rc.getID());
        MapLocation targetCell = new MapLocation(-1, -1);
        int targetTurnsSpent = 0;
        boolean exactTarget = false;
        while (true) {
            if (rng.nextInt(60) == 10) System.out.println("e");
            ++targetTurnsSpent;
            try {
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
                }
                else {
                    if (rc.canPickupFlag(rc.getLocation()) && rc.senseNearbyFlags(1, rc.getTeam().opponent()).length>=1){
                        rc.pickupFlag(rc.getLocation());
                        rc.setIndicatorString("Holding a flag!");
                    }
                    boolean adjFlag = false;
                    MapLocation potentialCarrier = null;
                    FlagInfo[] nearbyFlags = rc.senseNearbyFlags(9, rc.getTeam().opponent());
                    for (FlagInfo flag : nearbyFlags) {
                        if (flag.isPickedUp()) {
                            adjFlag = true;
                            potentialCarrier = flag.getLocation();
                        }
                    }
                    if (rc.hasFlag()) adjFlag = false;
                    MapLocation[] possibleCrumbs = rc.senseNearbyCrumbs(-1);
                    MapInfo[] possibleInfos = rc.senseNearbyMapInfos();
                    MapLocation[] possibleSenses = rc.senseBroadcastFlagLocations();
                    FlagInfo[] possibleFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                    if (targetCell.distanceSquaredTo(rc.getLocation()) < 1 || (targetCell.distanceSquaredTo(rc.getLocation()) < 6 && !exactTarget) || targetTurnsSpent > 10+rng.nextInt(5) || adjFlag) {
                        targetCell = new MapLocation(-1, -1);
                        targetTurnsSpent = 0;
                    }
                    if (targetCell.x == -1) {
                        if ((rc.hasFlag()) && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS){
                            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                            int mn = 1000000000;
                            for (int i = 0; i < spawnLocs.length; i++) {
                                int dist = spawnLocs[i].distanceSquaredTo(rc.getLocation());
                                if (dist < mn) {
                                    mn = dist;
                                    targetCell = spawnLocs[i];
                                }
                            }
                            exactTarget = true;
                        } else if (possibleFlags.length >= 1) {
                            FlagInfo targetFlag = possibleFlags[0];
                            targetCell = targetFlag.getLocation();
                            exactTarget = true;
                        } else if (possibleCrumbs.length >= 1) {
                            targetCell = possibleCrumbs[0];
                            exactTarget = true;
                        } else if (possibleSenses.length >= 1) {
                            targetCell = possibleSenses[0];
                            exactTarget = false;
                        } else {
                            targetCell = possibleInfos[rng.nextInt(possibleInfos.length)].getMapLocation();
                            exactTarget = false;
                        }
                    }
                    rc.setIndicatorString("Going to " + String.valueOf(targetCell.x) + " " + String.valueOf(targetCell.y));
                    RobotInfo[] possibleEnemies = rc.senseNearbyRobots(4, rc.getTeam().opponent());
                    if (possibleEnemies.length >= 1 && rc.canAttack(possibleEnemies[0].getLocation())) {
                        //System.out.println("Attacking");
                        rc.setIndicatorString("Attacking " + String.valueOf(possibleEnemies[0].getLocation().x) + " " + String.valueOf(possibleEnemies[0].getLocation().y));
                        rc.attack(possibleEnemies[0].getLocation());
                    }
                    if (!adjFlag) {
                        moveBetter(rc, targetCell);
                    }
                    for (RobotInfo ally : rc.senseNearbyRobots(-1, rc.getTeam())) {
                        if (rc.canHeal(ally.getLocation())) {
                            rc.heal(ally.getLocation());
                            break;
                        }
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
                Clock.yield();
            }
        }
    }
    public static Direction moveTowards(RobotController rc, MapLocation target) {
        // Assume no obstacles exist
        MapLocation loc = rc.getLocation();
        Direction newdir;
        if (target.y > loc.y) {
            if (target.x > loc.x) {
                newdir = Direction.NORTHEAST;
            } else if (target.x < loc.x) {
                newdir = Direction.NORTHWEST;
            } else {
                newdir = Direction.NORTH;
            }
        } else if (target.y < loc.y) {
            if (target.x > loc.x) {
                newdir = Direction.SOUTHEAST;
            } else if (target.x < loc.x) {
                newdir = Direction.SOUTHWEST;
            } else {
                newdir = Direction.SOUTH;
            }
        } else {
            if (target.x > loc.x) {
                newdir = Direction.EAST;
            } else if (target.x < loc.x) {
                newdir = Direction.WEST;
            } else {
                newdir = directions[rng.nextInt(directions.length)];
            }
        }
        return newdir;
    }
    static Direction[] stack = new Direction[10];
    static int stackSize = 0;
    static void moveBetter(RobotController rc, MapLocation pos) throws GameActionException {
        if(stackSize != 0 && (!rc.getLocation().directionTo(pos).equals(stack[0]) || rng.nextInt(32) == 0)) stackSize = 0;
        if(stackSize == 0) stack[stackSize++] = rc.getLocation().directionTo(pos);
        if(stackSize >= 2 && rc.canMove(stack[stackSize - 2])) stackSize--;
        while(stackSize < 8 && !rc.canMove(stack[stackSize - 1]) && !rc.canFill(rc.getLocation().add(stack[stackSize - 1]))) {
            stack[stackSize] = stack[stackSize - 1].rotateLeft();
            stackSize++;
        }
        if(stackSize >= 8) stackSize = 1;
        if(rc.canFill(rc.getLocation().add(stack[stackSize - 1]))) rc.fill(rc.getLocation().add(stack[stackSize - 1]));
        if(rc.canMove(stack[stackSize - 1])) rc.move(stack[stackSize - 1]);
    }
    public static void updateEnemyRobots(RobotController rc) throws GameActionException{
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            if (rc.canWriteSharedArray(0, enemyRobots.length)){
                rc.writeSharedArray(0, enemyRobots.length);
                int numEnemies = rc.readSharedArray(0);
            }
        }
    }
}