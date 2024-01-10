package StunBot;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.lang.Math;

public strictfp class RobotPlayer {
    enum Role {
        NONE,
        OWNER,
        HEALER,
        ATTACKER
    }

    static RobotController rc;
    static Role role = Role.NONE;
    static TrapType nextTrap = TrapType.NONE;
    static MapLocation home;
    static boolean chasing = false;
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

    /**
     * Attacks a nearby enemy if possible.
     * Prioritises enemies holding the flag.
     * If there are none, attacks the one with lowest health.
     * @throws GameActionException
     */
    static void attack() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemies.length == 0) {
            return;
        }
        RobotInfo target = enemies[0];
        for (RobotInfo enemy: enemies) {
            if (enemy.hasFlag()) {
                if (rc.canAttack(enemy.location)) {
                    rc.attack(enemy.location);
                    rc.setIndicatorString("I attacked the flag duck!");
                    return;
                }
            }
            if (enemy.getHealth() < target.getHealth()) {
                target = enemy;
            }
        }
        if (rc.canAttack(target.getLocation())) {
            rc.attack(target.getLocation());
        }
    }

    /**
     * Help function which checks whether a location is a centre spawn.
     * @param loc location to check
     * @return true if the location is a centre spawn
     */
    static boolean isCentreSpawn(MapLocation loc) {
        boolean isSpawn = false;
        for (MapLocation spawn: rc.getAllySpawnLocations()) {
            if (loc.distanceSquaredTo(spawn) == 4) {
                return false;
            }
            if (loc == spawn) {
                isSpawn = true;
            }
        }
        return isSpawn;
    }

    /**
     * Helper function to get all adjacent tiles to a given location.
     * @param loc location
     * @return a MapLocation array of length 8
     */
    static MapLocation[] adj(MapLocation loc) {
        shuffle();
        MapLocation[] ret = new MapLocation[8];
        for (int i = 0; i < 8; ++i) {
            ret[i] = loc.add(directions[i]);
        }
        return ret;
    }

    /**
     * Helper function to get all adjacent tiles of the current tile.
     * @return a MapLocation array of length 8
     */
    static MapLocation[] adj() {
        return adj(rc.getLocation());
    }

    /**
     * Randomly assigns the next trap to either STUN or EXPLOSIVE.
     * 70% chance STUN
     * 30% chance EXPLOSIVE
     * @return
     */
    static TrapType getNextTrap() {
        if (rng.nextInt(100) < 70) {
            return TrapType.STUN;
        } else {
            return TrapType.EXPLOSIVE;
        }
    }

    /**
     * Attempts to spawn a robot for the first time.
     * If possible, assigns it as the owner to the spawn.
     * @throws GameActionException
     */
    static void attemptSpawn() throws GameActionException {
        MapLocation[] spawns = rc.getAllySpawnLocations();
        for (MapLocation spawn: spawns) {
            if (isCentreSpawn(spawn)) {
                if (rc.canSpawn(spawn)) {
                    role = Role.OWNER;
                    home = spawn;
                    nextTrap = getNextTrap();
                    rc.spawn(spawn);
                    return;
                }
            }
        }
        for (MapLocation spawn: spawns) {
            if (isCentreSpawn(spawn)) {
                for (MapLocation next : adj(spawn)) {
                    if (rc.canSpawn(next)) {
                        rc.spawn(next);
                        if (rng.nextInt(100) < 50) {
                            role = Role.ATTACKER;
                        } else {
                            role = Role.HEALER;
                        }
                        home = spawn;
                        return;
                    }
                }
            }
        }
    }

    /**
     * Attempts to respawn a robot.
     * If there is an emergency, spawns it as close as possible to the emergency.
     * Else, spawns it at its home.
     * @throws GameActionException
     */
    static void attemptRespawn() throws GameActionException {
        if (role == Role.OWNER) {
            if (rc.canSpawn(home)) {
                rc.spawn(home);
            } else {
                shuffle();
                for (Direction dir: directions) {
                    MapLocation next = home.add(dir);
                    if (rc.canSpawn(next)) {
                        rc.spawn(next);
                    }
                }
            }
        } else {
            final MapLocation target;
            if (rc.readSharedArray(0) == MAX_ARRAY) {
                target = home;
            } else {
                target = read();
            }
            MapLocation[] spawns = rc.getAllySpawnLocations();
            Arrays.sort(spawns, Comparator.comparingInt((a) -> { return target.distanceSquaredTo(a); }));
            for (MapLocation spawn: spawns) {
                if (rc.canSpawn(spawn) && !isCentreSpawn(spawn)) {
                    rc.spawn(spawn);
                    return;
                }
            }
        }
    }

    /**
     * Attempts to build traps in a 5x5 square centred on the nearest spawn.
     * @throws GameActionException
     */
    static void trapSpawn() throws GameActionException {
        for (MapInfo mapInfo : rc.senseNearbyMapInfos(GameConstants.INTERACT_RADIUS_SQUARED)) {
            MapLocation loc = mapInfo.getMapLocation();
            if (closestSpawn(loc) <= 2) {
                if (rc.canBuild(nextTrap, loc)) {
                    rc.build(nextTrap, loc);
                    nextTrap = getNextTrap();
                }
            }
        }
    }

    static void heal() throws GameActionException {
        RobotInfo[] teammates = rc.senseNearbyRobots(-1, rc.getTeam());
        Arrays.sort(teammates, Comparator.comparingInt((a) -> { return a.getHealth(); }));
        for (RobotInfo teammate: teammates) {
            if (rc.canHeal(teammate.getLocation())) {
                rc.heal(teammate.getLocation());
                return;
            }
        }
    }

    /**
     * Optimised random walk to find all crumbs.
     * @throws GameActionException
     */
    static void findCrumbs() throws GameActionException {
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
                target = new MapLocation(Math.max(3, Math.min(rc.getMapWidth() - 4, rc.getLocation().x + rng.nextInt(21) - 10)),
                        Math.max(3, Math.min(rc.getMapHeight() - 4, rc.getLocation().y + rng.nextInt(21) - 10)));
                movesLeft = 7;
            }
            nextLoc = target;
        }
        moveTowards(nextLoc);
    }

    /**
     * Helper function which broadcasts that this spawn needs help.
     * @throws GameActionException
     */
    static void help(MapLocation loc) throws GameActionException {
        write(loc);
        rc.writeSharedArray(1, rc.getRoundNum() + 50);
        rc.setIndicatorString("Pls help!");
    }

    static void runOwner() throws GameActionException {
        if (GameConstants.SETUP_ROUNDS - rc.getRoundNum() <= 10) {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] friendly = rc.senseNearbyRobots(-1, rc.getTeam());
            chasing = false;
            for (RobotInfo enemy: enemies) {
                if (enemy.hasFlag()) {
                    help(enemy.getLocation());
                    moveTowards(enemy.getLocation());
                    rc.setIndicatorLine(rc.getLocation(), enemy.getLocation(), 255, 0, 0);
                    if (rc.getHealth() > 150) {
                        attack();
                    }
                    chasing = true;
                    break;
                }
            }
            if (!chasing) {
                if (rc.senseNearbyFlags(-1, rc.getTeam()).length > 0 && enemies.length - friendly.length >= 3) {
                    help(home);
                }
                if (!rc.getLocation().isWithinDistanceSquared(home, 2)) {
                    moveTowards(home);
                }
                for (int i = 0; i < 100; ++i) {
                    MapLocation spawn = rc.getAllySpawnLocations()[rng.nextInt(27)];
                    if (spawn.isWithinDistanceSquared(rc.getLocation(), 2)) {
                        Direction dir = rc.getLocation().directionTo(spawn);
                        if (rc.canMove(dir)) {
                            rc.move(dir);
                            rc.setIndicatorLine(rc.getLocation(), spawn, 0, 0, 255);
                            break;
                        }
                    }
                }
            }
            trapSpawn();
            heal();
            attack();
        }
    }

    static void runAttacker() throws GameActionException {
        if (GameConstants.SETUP_ROUNDS - rc.getRoundNum() >= 50) {
            findCrumbs();
        } else {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            for (FlagInfo flagInfo: rc.senseNearbyFlags(-1, rc.getTeam())) {
                if (!isCentreSpawn(flagInfo.getLocation())) {
                    moveTowards(flagInfo.getLocation());
                    rc.setIndicatorLine(rc.getLocation(), flagInfo.getLocation(), 255, 0, 0);
                    break;
                }
            }
            if (rc.readSharedArray(0) != MAX_ARRAY) {
                if (rc.getRoundNum() == rc.readSharedArray(1)) {
                    rc.writeSharedArray(0, MAX_ARRAY);
                } else {
                    MapLocation target = read();
                    rc.setIndicatorString("Rushing to help!");
                    moveTowards(target);
                }
            } else {
//                if (enemies.length > 0) {
//                    for (RobotInfo enemy : enemies) {
//                        Direction dir = rc.getLocation().directionTo(enemy.location);
//                        if (rc.canMove(dir)) {
//                            if (rc.getLocation().add(dir).isWithinDistanceSquared(home, 40)) {
//                                rc.move(dir);
//                                break;
//                            }
//                        }
//                    }
//                }
            }
            if (!rc.getLocation().isWithinDistanceSquared(home, 8)) {
                moveTowards(home);
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
//                    rc.setIndicatorString("I'm stuck in spawn :(");
                    for (Direction dir : directions) {
                        if (rc.canMove(dir)) {
                            rc.move(dir);
                        }
                    }
                }
            }
            for (Direction dir : directions) {
                if (rc.canMove(dir)) {
                    MapLocation next = rc.getLocation().add(dir);
                    int closest = closestSpawn(next);
                    if (closest > 0 && closest <= 2) {
                        rc.move(dir);
                        rc.setIndicatorString("Moved, within " + closest);
                        break;
                    }
                }
            }
            attack();
            heal();
        }
    }

    static void runHealer() throws GameActionException {
        if (GameConstants.SETUP_ROUNDS - rc.getRoundNum() >= 50) {
            findCrumbs();
        } else {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            for (RobotInfo enemy: enemies) {
                if (enemy.hasFlag()) {
                    moveTowards(enemy.getLocation());
                    rc.setIndicatorLine(rc.getLocation(), enemy.getLocation(), 255, 0, 0);
                    attack();
                    break;
                }
            }
            if (rc.readSharedArray(0) != MAX_ARRAY) {
                if (rc.getRoundNum() == rc.readSharedArray(1)) {
                    rc.writeSharedArray(0, MAX_ARRAY);
                } else {
                    MapLocation target = read();
                    rc.setIndicatorLine(rc.getLocation(), target, 0, 255, 0);
                    moveTowards(target);
                }
            } else {
//                if (enemies.length > 0) {
//                    for (RobotInfo enemy : enemies) {
//                        Direction dir = rc.getLocation().directionTo(enemy.location);
//                        if (rc.canMove(dir)) {
//                            if (rc.getLocation().add(dir).isWithinDistanceSquared(home, 30)) {
//                                rc.move(dir);
//                                break;
//                            }
//                        }
//                    }
//                }
            }
            if (!rc.getLocation().isWithinDistanceSquared(home, 8)) {
                moveTowards(home);
                rc.setIndicatorString("Going home!");
            }
            shuffle();
            if (closestSpawn(rc.getLocation()) == 0) {
                boolean moved = false;
                for (Direction dir : directions) {
                    if (rc.canMove(dir)) {
                        MapLocation next = rc.getLocation().add(dir);
                        if (closestSpawn(next) > 0) {
                            rc.move(dir);
                            rc.setIndicatorString("Moved out of spawn");
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
            for (Direction dir : directions) {
                if (rc.canMove(dir)) {
                    MapLocation next = rc.getLocation().add(dir);
                    int closest = closestSpawn(next);
                    if (closest > 0 && closest <= 2) {
                        rc.move(dir);
                        rc.setIndicatorString("Moved, within " + closest);
                        break;
                    }
                }
            }
            heal();
            attack();
        }
    }

    /**
     * Attempts to buy the next upgrade.
     * Priority order: ACTION, HEALING
     * @throws GameActionException
     */
    static void buyUpgrades() throws GameActionException {
        if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
            rc.buyGlobal(GlobalUpgrade.ACTION);
        } else if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
            rc.buyGlobal(GlobalUpgrade.HEALING);
        }
    }

    public static void run(RobotController _rc) throws GameActionException {
        rc = _rc;
        team = rc.getTeam() == Team.A ? 1 : 2;
        rng = new Random(rc.getID());
        if (rc.readSharedArray(0) == 0) {
            rc.writeSharedArray(0, MAX_ARRAY);
        }
        while (true) {
            try {
                if (!rc.isSpawned()) {
                    if (role == Role.NONE) {
                        attemptSpawn();
                    } else {
                        attemptRespawn();
                    }
                } else {
                    buyUpgrades();
                    if (role == Role.OWNER) {
                        runOwner();
                    } else if (role == Role.ATTACKER) {
                        runAttacker();
                    } else if (role == Role.HEALER) {
                        runHealer();
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
