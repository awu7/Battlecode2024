package StunBot;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.lang.Math;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int turnCount = 0;
    static Random rng;
    static boolean isDefender;
    static final int MAX_ARRAY = 65535;
    static int movesLeft = 0;
    static MapLocation target;

    static Direction[] stack = new Direction[10];
    static int stackSize = 0;

    static void moveTowards(MapLocation pos) throws GameActionException {
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

    static void shuffle() {
        for (int i = 7; i > 0; --i) {
            int j = rng.nextInt(i + 1);
            Direction temp = directions[i];
            directions[i] = directions[j];
            directions[j] = temp;
        }
    }

    static int closestSpawn(MapLocation loc) {
        int closest = 1000;  // infinity
        for (MapLocation spawnLoc : rc.getAllySpawnLocations()) {
            int dist = loc.distanceSquaredTo(spawnLoc);
            if (dist < closest) {
                closest = dist;
            }
        }
        return closest;
    }

    static MapLocation getClosestSpawn(MapLocation loc) {
        int closest = 1000;  // infinity
        MapLocation ans = rc.getAllySpawnLocations()[0];
        for (MapLocation spawnLoc : rc.getAllySpawnLocations()) {
            int dist = loc.distanceSquaredTo(spawnLoc);
            if (dist < closest) {
                closest = dist;
                ans = spawnLoc;
            }
        }
        return ans;
    }

    static void write(MapLocation loc) throws GameActionException {
        int n = loc.x * 61 + loc.y;
        rc.writeSharedArray(0, n);
    }

    static MapLocation read() throws GameActionException {
        int n = rc.readSharedArray(0);
        return new MapLocation(n / 61, n % 61);
    }

    static void attack(RobotInfo[] enemies) throws GameActionException {
        if (enemies.length == 0) {
            return;
        }
        int minHealth = 1000;
        MapLocation target = enemies[0].getLocation();
        for (RobotInfo enemy: enemies) {
            if (enemy.getHealth() < minHealth) {
                minHealth = enemy.health;
                target = enemy.getLocation();
            }
        }
        if (rc.canAttack(target)) {
            rc.attack(target);
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController _rc) throws GameActionException {
        rc = _rc;
        rng = new Random(rc.getID());
        isDefender = rng.nextInt() % 100 < 70;
        if (rc.readSharedArray(0) == 0) {
            rc.writeSharedArray(0, MAX_ARRAY);
        }
        while (true) {
            turnCount += 1;

            try {
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    if (rc.readSharedArray(0) == MAX_ARRAY) {
                        for (int i = 0; i < 100; ++i) {
                            MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                            if (rc.canSpawn(randomLoc)) {
                                rc.spawn(randomLoc);
                                break;
                            }
                        }
                    } else {
                        MapLocation target = getClosestSpawn(read());
                        for (MapLocation spawnLoc: spawnLocs) {
                            if (spawnLoc.isWithinDistanceSquared(target, 5)) {
                                if (rc.canSpawn(spawnLoc)) {
                                    rc.spawn(spawnLoc);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    if (turnCount <= GameConstants.SETUP_ROUNDS) {
                        MapLocation nextLoc;
                        MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
                        if(crumbs.length > 0) {
                            // sort crumbs by distance
                            Arrays.sort(crumbs, (a, b) -> {
                                return rc.getLocation().distanceSquaredTo(a) - rc.getLocation().distanceSquaredTo(b);
                            });
                            nextLoc = crumbs[0];
                        } else {
                            if(movesLeft > 0 && !rc.getLocation().equals(target)) {
                                movesLeft--;
                            } else {
                                target = new MapLocation(Math.max(0, Math.min(rc.getMapWidth(), rc.getLocation().x + rng.nextInt(21) - 10)),
                                        Math.max(0, Math.min(rc.getMapHeight(), rc.getLocation().y + rng.nextInt(21) - 10)));
                                movesLeft = 7;
                            }
                            nextLoc = target;
                        }
                        moveTowards(nextLoc);
                    } else {
                        if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
                            rc.buyGlobal(GlobalUpgrade.ACTION);
                        }
                        MapLocation[] spawns = rc.getAllySpawnLocations();
                        MapInfo[] mapInfos = rc.senseNearbyMapInfos(-1);
                        int team;
                        if (rc.getTeam() == Team.A) {
                            team = 1;
                        } else {
                            team = 2;
                        }
                        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                        for (RobotInfo enemy : enemies) {
                            if (rc.canAttack(enemy.getLocation())) {
                                rc.attack(enemy.getLocation());
                                break;
                            }
                        }
                        for (MapInfo mapInfo : mapInfos) {
                            if (mapInfo.getSpawnZoneTeam() == team) {
                                if (mapInfo.getTrapType() == TrapType.NONE) {
                                    MapLocation loc = mapInfo.getMapLocation();
                                    shuffle();
                                    for (Direction dir : directions) {
                                        MapLocation next = loc.add(dir);
                                        if (rc.canBuild(TrapType.EXPLOSIVE, next)) {
                                            if (rng.nextInt() % 100 < 30) {
                                                rc.build(TrapType.STUN, next);
                                            } else {
                                                rc.build(TrapType.EXPLOSIVE, next);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (rc.senseNearbyFlags(-1).length == 0) {
                            isDefender = false;
                        } else if (turnCount > 200) {
                            isDefender = true;
                            if (enemies.length > 5) {
                                write(rc.getLocation());
                                rc.writeSharedArray(1, 50);
                                rc.setIndicatorString("Pls help!");
                            }
                        }
                        if (rc.readSharedArray(0) != MAX_ARRAY) {
                            int count = rc.readSharedArray(1);
                            count--;
                            if (count == 0) {
                                rc.writeSharedArray(0, MAX_ARRAY);
                            }
                            rc.writeSharedArray(1, count);
                        }
                        if (enemies.length == 0) {
                            isDefender = false;
                        } else if (rc.readSharedArray(0) == MAX_ARRAY) {
                            for (RobotInfo enemy : enemies) {
                                Direction dir = rc.getLocation().directionTo(enemy.location);
                                if (rc.canMove(dir)) {
                                    rc.move(dir);
                                    break;
                                }
                            }
                        }
                        shuffle();
                        if (closestSpawn(rc.getLocation()) == 0) {
                            boolean moved = false;
                            for (Direction dir : directions) {
                                if (rc.canMove(dir)) {
                                    MapLocation next = rc.getLocation().add(dir);
                                    if (closestSpawn(next) > 0) {
                                        rc.move(dir);
                                        moved = true;
                                    }
                                }
                            }
                            if (!moved) {
                                rc.setIndicatorString("I'm stuck in spawn :(");
                                for (Direction dir : directions) {
                                    if (rc.canMove(dir)) {
                                        rc.move(dir);
                                    }
                                }
                            }
                        }
                        if (isDefender) {
                            for (Direction dir : directions) {
                                if (rc.canMove(dir)) {
                                    MapLocation next = rc.getLocation().add(dir);
                                    int closest = closestSpawn(next);
                                    if (closest > 0 && closest <= 6) {
                                        rc.move(dir);
                                        rc.setIndicatorString("Moved, within " + closest);
                                        break;
                                    }
                                }
                            }
                            for (Direction dir : directions) {
                                if (rc.canMove(dir)) {
                                    MapLocation next = rc.getLocation().add(dir);
                                    if (closestSpawn(next) > 0) {
                                        rc.move(dir);
                                    }
                                }
                            }
                        } else {
                            if (rc.readSharedArray(0) == MAX_ARRAY) {
                                MapLocation closest = getClosestSpawn(rc.getLocation());
                                moveTowards(closest);
                            } else {
                                MapLocation target = read();
                                rc.setIndicatorString("Rushing to help at (" + target.x + ", " + target.y + ")!");
                                moveTowards(target);
                            }
                        }
                        attack(enemies);
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
