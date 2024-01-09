package RushBot;

import battlecode.common.*;

import java.util.*;

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
    static int turnCount = 0;
    public static void run(RobotController _rc) throws GameActionException {
        rc = _rc;
        rng = new Random(rc.getID());
        MapLocation targetCell = new MapLocation(-1, -1);
        int targetTurnsSpent = 0;
        boolean exactTarget = false;
        while (true) {
            turnCount++;
            if (rng.nextInt(200) == 10) System.out.println("e");
            ++targetTurnsSpent;
            try {
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
                }
                else if(!rc.hasFlag() && rc.senseNearbyFlags(0, rc.getTeam().opponent()).length >= 1 && !rc.canPickupFlag(rc.getLocation())) {
                    // wait, we need to pick up a flag dropped by a teammate
                } else if(rc.hasFlag() && rc.senseMapInfo(rc.getLocation()).getSpawnZoneTeam() == (rc.getTeam() == Team.A ? 1 : 2)) {
                    // wait, we need to drop off a flag at our spawn zone
                } else {
                    if (rc.canPickupFlag(rc.getLocation()) && turnCount >= GameConstants.SETUP_ROUNDS){
                        rc.pickupFlag(rc.getLocation());
                        targetCell = new MapLocation(-1, -1); // retarget now that we have the flag
                        targetTurnsSpent = 0;
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
                        moveBetter(targetCell);
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
    static Direction[] stack = new Direction[10];
    static int stackSize = 0;
    static int turnDir = 0;
    static void moveBetter(MapLocation pos) throws GameActionException {
        if(stackSize != 0 && (!rc.getLocation().directionTo(pos).equals(stack[0]) || rng.nextInt(8) == 0)) stackSize = 0;
        if(stackSize == 0) stack[stackSize++] = rc.getLocation().directionTo(pos);
        if(stackSize == 1) turnDir = rng.nextInt(2);
        if(stackSize >= 2 && rc.canMove(stack[stackSize - 2])) stackSize--;
        boolean moveCooldownDone = rc.getMovementCooldownTurns() == 0;
        MapLocation nextLoc;
        RobotInfo nextLocRobot;
        while(stackSize < 8) {
            nextLoc = rc.getLocation().add(stack[stackSize - 1]);
            if(rc.onTheMap(nextLoc)) {
                if(!moveCooldownDone) {
                    if(!rc.senseMapInfo(nextLoc).isWall()) break;
                } else {
                    if(rc.canMove(stack[stackSize - 1]) || rc.senseMapInfo(nextLoc).isWater()) break;
                }
                nextLocRobot = rc.senseRobotAtLocation(nextLoc);
                if(rc.hasFlag() && rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.team == rc.getTeam()) break;
            }
            stack[stackSize] = turnDir == 0 ? stack[stackSize - 1].rotateLeft() : stack[stackSize - 1].rotateRight();
            stackSize++;
        }
        if(stackSize >= 8) {
            stackSize = 1;
        }
        nextLoc = rc.getLocation().add(stack[stackSize - 1]);
        nextLocRobot = rc.senseRobotAtLocation(nextLoc);
        if(rc.canFill(nextLoc)) rc.fill(nextLoc);
        if(rc.canMove(stack[stackSize - 1])) rc.move(stack[stackSize - 1]);
        if(rc.hasFlag() && rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.team == rc.getTeam()) {
            rc.dropFlag(nextLoc);
        }
    }
}
