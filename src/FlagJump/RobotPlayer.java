package FlagJump;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Random rng;
    static final int MAX_ARRAY = 65535;
    static int movesLeft = 0;
    static MapLocation target;
    static Direction[] stack = new Direction[10];
    static int stackSize = 0;
    static int team = 0;
    static Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    static boolean gotFlag = false;

    static void moveTowards(MapLocation pos) throws GameActionException {
        rc.setIndicatorLine(rc.getLocation(), pos, 0, 255, 0);
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

    static void flagJump(MapLocation pos) throws GameActionException {
        rc.setIndicatorLine(rc.getLocation(), pos, 0, 255, 0);
        if(stackSize != 0 && (!rc.getLocation().directionTo(pos).equals(stack[0]) || rng.nextInt(8) == 0)) stackSize = 0;
        if(stackSize == 0) stack[stackSize++] = rc.getLocation().directionTo(pos);
        if(stackSize >= 2 && rc.canMove(stack[stackSize - 2])) stackSize--;
        while(stackSize < 8 && !rc.canMove(stack[stackSize - 1])) {
            stack[stackSize] = stack[stackSize - 1].rotateLeft();
            stackSize++;
        }
        if(stackSize >= 8) stackSize = 1;
        if(rc.canMove(stack[stackSize - 1])) {
            Direction dir = stack[stackSize - 1];
            MapLocation old = rc.getLocation();
            if (rc.hasFlag()) {
                rc.dropFlag(old.add(dir));
            }
            rc.move(stack[stackSize - 1]);
            if (rc.canPickupFlag(old)) {
                rc.pickupFlag(old);
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

    public static void run(RobotController _rc) throws GameActionException {
        rc = _rc;
        team = rc.getTeam() == Team.A ? 1 : 2;
        rng = new Random(rc.getID());
        if (rc.readSharedArray(0) == 1) {
            return;
        }
        while (true) {
            try {
                if (!rc.isSpawned()) {
                    if (rc.canSpawn(rc.getAllySpawnLocations()[0])) {
                        rc.spawn(rc.getAllySpawnLocations()[0]);
                        rc.writeSharedArray(0, 1);
                    }
                } else {
                    if (rc.getRoundNum() > 200) {
                        if (!gotFlag) {
                            if (rc.senseNearbyFlags(-1, rc.getTeam().opponent()).length > 0) {
                                if (rc.canPickupFlag(rc.senseNearbyFlags(-1, rc.getTeam().opponent())[0].getLocation())) {
                                    rc.pickupFlag(rc.senseNearbyFlags(-1, rc.getTeam().opponent())[0].getLocation());
                                    gotFlag = true;
                                } else {
                                    moveTowards(rc.senseNearbyFlags(-1, rc.getTeam().opponent())[0].getLocation());
                                }
                            } else {
                                if (rc.senseBroadcastFlagLocations().length > 0 && rc.getLocation() != rc.senseBroadcastFlagLocations()[0]) {
                                    moveTowards(rc.senseBroadcastFlagLocations()[0]);
                                } else {
                                    shuffle();
                                    for (Direction dir : directions) {
                                        if (rc.canMove(dir)) {
                                            rc.move(dir);
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            if (rc.getLocation().equals(rc.getAllySpawnLocations()[0])) {
                                if (rc.canPickupFlag(rc.getLocation())) {
                                    rc.pickupFlag(rc.getLocation());
                                }
                                gotFlag = false;
                            } else {
                                flagJump(rc.getAllySpawnLocations()[0]);
                            }
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
                Clock.yield();
            }
        }
    }
}
