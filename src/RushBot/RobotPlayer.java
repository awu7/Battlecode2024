package RushBot;

import battlecode.common.*;

import java.util.*;
public strictfp class RobotPlayer {
    static Random rng = new Random(6147);
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
            ++targetTurnsSpent;

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
                if (!rc.isSpawned()){
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    // Pick a random spawn location to attempt spawning in.
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
                }
                else{ // Robot is spawned
                    if (rc.canPickupFlag(rc.getLocation()) && rc.senseNearbyFlags(1, rc.getTeam().opponent()).length>=1){
                        rc.pickupFlag(rc.getLocation());
                        rc.setIndicatorString("Holding a flag!");
                    }
                    // If we are holding an enemy flag, singularly focus on moving towards
                    // an ally spawn zone to capture it! We use the check roundNum >= SETUP_ROUNDS
                    // to make sure setup phase has ended.
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
                    // Move and attack randomly if no objective.
                    MapLocation[] possibleCrumbs = rc.senseNearbyCrumbs(-1);
                    MapInfo[] possibleInfos = rc.senseNearbyMapInfos();
                    MapLocation[] possibleSenses = rc.senseBroadcastFlagLocations();
                    FlagInfo[] possibleFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                    Direction newdir;
                    if (targetCell.distanceSquaredTo(rc.getLocation()) < 1 || (targetCell.distanceSquaredTo(rc.getLocation()) < 6 && !exactTarget) || targetTurnsSpent > 10+rng.nextInt(5) || adjFlag) {
                        targetCell = new MapLocation(-1, -1);
                        targetTurnsSpent = 0;
                    }
                    if (targetCell.x == -1) {
                        if (adjFlag) {
                            targetCell = rc.getLocation().add(moveTowards(rc, potentialCarrier).opposite());
                            exactTarget = true;
                        } else if ((rc.hasFlag()) && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS){
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
                        //rc.setIndicatorLine(rc.getLocation(), possibleEnemies[0].getLocation(), 255, 0, 0);
                    }
                    Direction dir = moveTowards(rc, targetCell);
                    MapLocation nextLoc = rc.getLocation().add(dir);
                    if (!adjFlag) {
                        moveBetter(rc, targetCell);
                    }
                    // Rarely attempt placing traps behind the robot.
                    MapLocation prevLoc = rc.getLocation().subtract(dir);
                    if (rc.canBuild(TrapType.EXPLOSIVE, prevLoc) && rng.nextInt() % 37 == 1)
                        rc.build(TrapType.EXPLOSIVE, prevLoc);
                    // We can also move our code into different methods or classes to better organize it!
                    updateEnemyRobots(rc);
                    for (RobotInfo ally : rc.senseNearbyRobots(-1, rc.getTeam())) {
                        if (rc.canHeal(ally.getLocation())) {
                            rc.heal(ally.getLocation());
                            break;
                        }
                    }
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                if (rc.onTheMap(targetCell) && rc.isSpawned()) {
                    rc.setIndicatorLine(rc.getLocation(), targetCell, 0, 255, 0);
                }
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
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
        if(stackSize != 0 && (!rc.getLocation().directionTo(pos).equals(stack[0]) || rng.nextInt(8) == 0)) stackSize = 0;
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
        // Sensing methods can be passed in a radius of -1 to automatically
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            //rc.setIndicatorString("There are nearby enemy robots! Scary!");
            // Save an array of locations with enemy robots in them for future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            // Let the rest of our team know how many enemy robots we see!
            if (rc.canWriteSharedArray(0, enemyRobots.length)){
                rc.writeSharedArray(0, enemyRobots.length);
                int numEnemies = rc.readSharedArray(0);
            }
        }
    }
}
