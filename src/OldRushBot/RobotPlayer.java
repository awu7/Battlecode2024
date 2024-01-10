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
        while(stackSize < 8) {
            nextLoc = rc.getLocation().add(stack[stackSize - 1]);
            if(rc.onTheMap(nextLoc)) {
                if(!moveCooldownDone) {
                    if(!rc.senseMapInfo(nextLoc).isWall() && (!rc.senseMapInfo(nextLoc).isWater() || !rc.hasFlag())) break;
                } else {
                    if(rc.canMove(stack[stackSize - 1]) || (rc.senseMapInfo(nextLoc).isWater() && !rc.hasFlag())) break;
                }
                nextLocRobot = rc.senseRobotAtLocation(nextLoc);
                if(rc.hasFlag() && rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.team == rc.getTeam()) break;
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
        if(rc.canFill(nextLoc)) rc.fill(nextLoc);
        if(rc.canMove(stack[stackSize - 1])) rc.move(stack[stackSize - 1]);
        if(rc.hasFlag() && rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.team == rc.getTeam()) {
            rc.dropFlag(nextLoc);
        }
    }
    static MapLocation swarmTarget;
    static MapLocation findTarget() throws GameActionException {
        // Targeting algorithm:
        // If we have the flag, go home
        // Go for flags
        // Protect flag bearers
        // If swarm is active, go to swarm target
        // If we see many allies, activate swarm
        // Go for crumbs
        // Go to the centre
        if(rc.hasFlag()) return closest(rc.getAllySpawnLocations());
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
        int swarmLeader = rc.readSharedArray(0);
        if(swarmLeader != 0 && !rc.onTheMap(swarmTarget)) {
            // System.out.println("Here");
            swarmTarget = new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2));
        }
        if(swarmLeader == rc.getID()) {
            rc.writeSharedArray(0, 0);
        }
        if(nearbyAllies.length < 4) swarmTarget = new MapLocation(-1, -1);
        if(rc.onTheMap(swarmTarget)) return swarmTarget;
        if(nearbyAllies.length >= 40) {
            System.out.println("Swarm activated");
            MapLocation[] possibleSenses = rc.senseBroadcastFlagLocations();
            swarmTarget = possibleSenses[rng.nextInt(possibleSenses.length)];
            rc.writeSharedArray(0, rc.getID());
            rc.writeSharedArray(1, swarmTarget.x);
            rc.writeSharedArray(2, swarmTarget.y);
            return swarmTarget;
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

    static int specialisation; // 0 healer, 1 attacker
    public static void run(RobotController _rc) throws GameActionException {
        rc = _rc;
        rng = new Random(rc.getID());
        specialisation = rng.nextInt(2);
        MapLocation targetCell = new MapLocation(-1, -1);
        centre = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        int lastSeenSwarm = 0;
        while (true) {
            try {
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    Arrays.sort(spawnLocs, (MapLocation a, MapLocation b) -> {
                        return centre.distanceSquaredTo(a) - centre.distanceSquaredTo(b);
                    });
                    for(MapLocation loc : spawnLocs) {
                        if(rc.canSpawn(loc)) {
                            rc.spawn(loc);
                            swarmTarget = new MapLocation(-1, -1);
                            break;
                        }
                    }
                } else if(!rc.hasFlag() && rc.senseNearbyFlags(0, rc.getTeam().opponent()).length >= 1 && !rc.canPickupFlag(rc.getLocation())) {
                    // wait, we need to pick up a flag dropped by a teammate
                } else {
                    if (rc.canPickupFlag(rc.getLocation()) && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS){
                        rc.pickupFlag(rc.getLocation());
                    }
                    targetCell = findTarget();
                    rc.setIndicatorString("Going to " + String.valueOf(targetCell.x) + " " + String.valueOf(targetCell.y));
                    moveBetter(targetCell);
                    // this is kinda broken, need to fix later
                    attackOrHeal();
                    // if(specialisation == 0) {
                    //     heal();
                    //     attack();
                    // } else {
                    //     attack();
                    //     heal();
                    // }
                    // Drop traps
                    if(rc.getCrumbs() >= 150) {
                        TrapType randTrap = new TrapType[]{TrapType.EXPLOSIVE, TrapType.STUN}[rng.nextInt(2)];
                        RobotInfo[] visibleEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                        if (rc.canBuild(randTrap, rc.getLocation()) && rng.nextInt(max(100 - (30*visibleEnemies.length), 3)) == 0) {
                            rc.build(randTrap, rc.getLocation());
                        }
                    }
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