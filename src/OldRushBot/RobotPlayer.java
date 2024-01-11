package OldRushBot;

import battlecode.common.*;
import battlecode.world.Flag;
import battlecode.world.Trap;

import java.awt.*;
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
    static MapLocation centre;
    static int movesLeft = 0;
    static MapLocation target;

    static int max(int a, int b) {
        if (a > b) {
            return a;
        }
        return b;
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
        boolean triedOtherDir = false;
        boolean hasFlag = rc.hasFlag() || rc.canPickupFlag(rc.getLocation());
        while(stackSize < 8) {
            nextLoc = rc.getLocation().add(stack[stackSize - 1]);
            if(rc.onTheMap(nextLoc)) {
                if(!moveCooldownDone) {
                    if(!rc.senseMapInfo(nextLoc).isWall() && (!rc.senseMapInfo(nextLoc).isWater() || !hasFlag)) break;
                } else {
                    if(rc.canMove(stack[stackSize - 1]) || (rc.senseMapInfo(nextLoc).isWater() && !hasFlag)) break;
                }
                nextLocRobot = rc.senseRobotAtLocation(nextLoc);
                if(hasFlag && nextLocRobot != null && nextLocRobot.team == rc.getTeam()) break;
            } else {
                // reset if hugging wall, try other turn dir
                stackSize = 1;
                if(triedOtherDir) break;
                turnDir = 1 - turnDir;
                triedOtherDir = true;
            }
            stack[stackSize] = turnDir == 0 ? stack[stackSize - 1].rotateLeft() : stack[stackSize - 1].rotateRight();
            stackSize++;
        }
        if(stackSize >= 8) {
            stackSize = 1;
        }
        nextLoc = rc.getLocation().add(stack[stackSize - 1]);
        nextLocRobot = rc.senseRobotAtLocation(nextLoc);
        if(rc.canFill(nextLoc) && !hasFlag) rc.fill(nextLoc);
        if(rc.canMove(stack[stackSize - 1])) {
            MapLocation old = rc.getLocation();
            if (rc.hasFlag() && rc.canDropFlag(rc.getLocation().add(stack[stackSize - 1]))) {
                rc.dropFlag(rc.getLocation().add(stack[stackSize - 1]));
            }
            rc.move(stack[stackSize - 1]);
            if (rc.canPickupFlag(old) && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
                rc.pickupFlag(old);
            }
        }
        if(hasFlag && rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.team == rc.getTeam()) {
            rc.dropFlag(nextLoc);
        }
    }

    static void broadcastSwarmTarget(MapLocation loc) throws GameActionException {
        rc.writeSharedArray(0, rc.getID());
        rc.writeSharedArray(1, loc.x);
        rc.writeSharedArray(2, loc.y);
    }

    static MapLocation swarmTarget;
    static int swarmStarted = 0;
    static MapLocation findTarget() throws GameActionException {
        // Targeting algorithm:
        // If we have the flag, go home
        // Chase opponent flag bearers (and call the swarm for help)
        // Go for flags
        // Protect flag bearers
        // If in combat, kite enemies
        // If swarm is active, go to swarm target
        // If we see many allies, activate swarm
        // Go for crumbs
        // Go to the centre
        int swarmLeader = rc.readSharedArray(0);
        if(swarmLeader == rc.getID()) {
            rc.writeSharedArray(0, 0);
        }
        if(rc.hasFlag() || rc.canPickupFlag(rc.getLocation())) return closest(rc.getAllySpawnLocations());
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
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        for(RobotInfo ally : nearbyAllies) {
            if(ally.hasFlag()) return ally.getLocation();
        }
        if(swarmLeader != 0 && !rc.onTheMap(swarmTarget)) {
            swarmTarget = new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2));
            swarmStarted = rc.getRoundNum();
        }
        if(nearbyAllies.length < 4 || swarmStarted < rc.getRoundNum() - rc.getMapHeight() - rc.getMapWidth()) swarmTarget = new MapLocation(-1, -1);
        if(rc.onTheMap(swarmTarget)) return swarmTarget;
        if(nearbyAllies.length >= 34) { // Changed from 40 (4/5) to 34 (2/3) Experimental
            System.out.println("Swarm activated");
            MapLocation[] possibleSenses = rc.senseBroadcastFlagLocations();
            if(possibleSenses.length > 0) {
                swarmTarget = possibleSenses[rng.nextInt(possibleSenses.length)];
                broadcastSwarmTarget(swarmTarget);
                return swarmTarget;
            }
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
    static void heal() throws GameActionException {
        // Also prioritise flag carriers when healing
        RobotInfo[] nearbyAllyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo healTarget = rc.senseRobot(rc.getID());
        for (RobotInfo ally : nearbyAllyRobots) {
            if(rc.canHeal(ally.getLocation())
                    && (ally.hasFlag() && !healTarget.hasFlag()
                    || (ally.hasFlag() == healTarget.hasFlag() && ally.health < healTarget.health))) {
                healTarget = ally;
            }
        }
        if(rc.canHeal(healTarget.getLocation())) {
            rc.heal(healTarget.getLocation());
        }
    }

    static void attackOrHeal() throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots();
        Arrays.sort(nearby, (a, b) -> { return a.getHealth() - b.getHealth(); });
        for (RobotInfo robot: nearby) {
            MapLocation loc = robot.getLocation();
            if (robot.getTeam() == rc.getTeam()) {
                if (rc.canHeal(loc)) {
                    rc.heal(loc);
                    break;
                }
            } else {
                if (rc.canAttack(loc)) {
                    rc.attack(loc);
                    break;
                }
            }
        }
    }

    static void pickupFlag() throws GameActionException {
        if(rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
            for(FlagInfo f : rc.senseNearbyFlags(-1, rc.getTeam().opponent())) {
                if(rc.canPickupFlag(f.getLocation()) && rc.senseNearbyRobots(f.getLocation(), 0, rc.getTeam()).length == 0) {
                    rc.pickupFlag(f.getLocation());
                    break;
                }
            }
        }
    }

    static void buildTraps() throws GameActionException {
        if (rc.getCrumbs() >= 150 && rc.getRoundNum() >= 200) {
            TrapType randTrap = new TrapType[]{TrapType.EXPLOSIVE, TrapType.EXPLOSIVE, TrapType.EXPLOSIVE, TrapType.STUN}[rng.nextInt(2)];
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

    public static void run(RobotController _rc) throws GameActionException {
        rc = _rc;
        rng = new Random(rc.getID());
        MapLocation targetCell = new MapLocation(-1, -1);
        centre = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        while (true) {
            try {
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    // Arrays.sort(spawnLocs, (MapLocation a, MapLocation b) -> {
                    //     return centre.distanceSquaredTo(a) - centre.distanceSquaredTo(b);
                    // });
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
                            break;
                        }
                    }
                } else if (rc.getRoundNum() <= 150) {
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
                            target = new MapLocation(Math.max(0, Math.min(rc.getMapWidth() - 1, rc.getLocation().x + rng.nextInt(21) - 10)),
                                    Math.max(0, Math.min(rc.getMapHeight() - 1, rc.getLocation().y + rng.nextInt(21) - 10)));
                            movesLeft = 7;
                        }
                        nextLoc = target;
                    }
                    if (!rc.onTheMap(nextLoc)) {
                        System.out.println(nextLoc.x + ", " + nextLoc.y);
                    }
                    moveBetter(nextLoc);
                } else if (!rc.hasFlag() && rc.senseNearbyFlags(0, rc.getTeam().opponent()).length >= 1 && !rc.canPickupFlag(rc.getLocation())) {
                    // wait, we need to pick up a flag dropped by a teammate
                } else {
                    pickupFlag();
                    targetCell = findTarget();
                    rc.setIndicatorString("Going to " + String.valueOf(targetCell.x) + " " + String.valueOf(targetCell.y));
                    attackOrHeal();
                    RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                    if (enemies.length > 0 && !rc.hasFlag() && rc.senseNearbyFlags(3, rc.getTeam().opponent()).length == 0) {
                        int minTargeted = 0;
                        Direction[] choices = new Direction[8];
                        MapLocation loc = rc.getLocation();
                        for (RobotInfo enemy : enemies) {
                            if (enemy.getLocation().isWithinDistanceSquared(loc, 4)) {
                                minTargeted++;
                            }
                        }
                        int n = 0;
                        shuffle();
                        for (Direction dir : directions) {
                            if (rc.canMove(dir)) {
                                int count = 0;
                                for (RobotInfo enemy : enemies) {
                                    if (enemy.getLocation().isWithinDistanceSquared(loc.add(dir), 4)) {
                                        count++;
                                    }
                                }
                                if (count < minTargeted) {
                                    minTargeted = count;
                                    n = 0;
                                }
                                if (count == minTargeted) {
                                    choices[n++] = dir;
                                }
                            }
                        }
                        if (n > 0) {
                            rc.setIndicatorString("Targeted by " + minTargeted);
                            MapLocation finalTargetCell = targetCell;
                            Arrays.sort(choices, 0, n, (a, b) -> {
                                return rc.getLocation().add(a).distanceSquaredTo(finalTargetCell) - rc.getLocation().add(b).distanceSquaredTo(finalTargetCell);
                            });
                            rc.move(choices[0]);
                        } else {
                            shuffle();
                            int minAdj = 0;
                            for (RobotInfo friend : rc.senseNearbyRobots(-1, rc.getTeam())) {
                                if (friend.getLocation().isWithinDistanceSquared(rc.getLocation(), 2)) {
                                    minAdj++;
                                }
                            }
                            Direction choice = Direction.NORTH;
                            boolean overriden = false;
                            for (Direction dir: directions) {
                                if (rc.canMove(dir)) {
                                    int adjCount = 0;
                                    for (RobotInfo friend : rc.senseNearbyRobots(-1, rc.getTeam())) {
                                        if (friend.getLocation().isWithinDistanceSquared(rc.getLocation().add(dir), 2)) {
                                            adjCount++;
                                        }
                                    }
                                    if (adjCount < minAdj) {
                                        choice = dir;
                                        minAdj = adjCount;
                                        overriden = true;
                                    }
                                }
                            }
                            if (overriden) {
                                rc.move(choice);
                            }
                        }
                    } else {
                        moveBetter(targetCell);
                    }
                    pickupFlag();
                    attackOrHeal();
                    buildTraps();
                    // Attempt to buy global upgrades
                    if (rc.getRoundNum() >= 750 && rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
                        rc.buyGlobal(GlobalUpgrade.ACTION);
                    }
                    if (rc.getRoundNum() >= 1500 && rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
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
                Clock.yield();
            }
        }
    }
}