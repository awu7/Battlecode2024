package TestBot;

import battlecode.common.*;

public class Attacking {
    public static boolean attacked = false;
    public static int idle = 0;

    public static void attackUtil(MapLocation loc) {
        try {
            V.rc.attack(loc);
            V.history.append('A');
            attacked = true;
            idle = 0;
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
    }

    public static boolean attackFlag() throws GameActionException {
        if (!V.rc.isActionReady()) {
            return false;
        }
        for (RobotInfo enemy: V.rc.senseNearbyRobots(11, V.team.opponent())) {
            if (enemy.hasFlag()) {
                MapLocation loc = enemy.getLocation();
                if (V.rc.canAttack(loc)) {
                    V.lastAttackTimestamp = V.round;
                    attackUtil(loc);
                    return true;
                }
                if (V.rc.isMovementReady()) {
                    Direction dir = V.rc.getLocation().directionTo(loc);
                    for (Direction choice: new Direction[]{dir, dir.rotateLeft(), dir.rotateRight()}) {
                        if (V.rc.adjacentLocation(choice).isWithinDistanceSquared(loc, 4) && V.rc.canMove(choice)) {
                            V.rc.move(choice);
                            if (V.rc.canAttack(loc)) {
                                V.lastAttackTimestamp = V.round;
                                V.rc.attack(loc);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static class AttackTarget {
        boolean canAttack;
        MapLocation loc;
        int dist;
        int hits;
        int levels;
        int id;

        public AttackTarget(RobotInfo robotInfo) {
            loc = robotInfo.getLocation();
            canAttack = V.rc.canAttack(loc);
            dist = V.rc.getLocation().distanceSquaredTo(loc);
            hits = (int) ((robotInfo.getHealth() - 1) / MicroAttacker.dps[V.rc.getLevel(SkillType.ATTACK)]) + 1;
            levels = 2 * robotInfo.getAttackLevel() * robotInfo.getAttackLevel() + 4 * robotInfo.getHealLevel() + robotInfo.getBuildLevel();
            id = robotInfo.getID();
        }

        public boolean isBetter(AttackTarget a) {
            if (canAttack != a.canAttack) {
                return canAttack;
            }
            if (levels != a.levels) {
                return levels > a.levels;
            }
            if (hits != a.hits) {
                return hits < a.hits;
            }
            if (dist != a.dist) {
                return dist < a.dist;
            }
            return id < a.id;
        }
    }

    public static boolean attack() {
        if (!V.rc.isActionReady()) {
            return false;
        }
        try {
            RobotInfo[] enemies;
            enemies = V.rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, V.team.opponent());
            if (enemies.length == 0) {
                return false;
            }
            int n = enemies.length;
            AttackTarget bestTarget = new AttackTarget(enemies[enemies.length - 1]);
            for (int i = n - 1; --i >= 0;) {
                AttackTarget a = new AttackTarget(enemies[i]);
                if (a.isBetter(bestTarget)) {
                    bestTarget = a;
                }
            }
            if (V.rc.canAttack(bestTarget.loc)) {
                V.lastAttackTimestamp = V.round;
                attackUtil(bestTarget.loc);
                return true;
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
        return false;
    }
}
