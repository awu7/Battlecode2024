package RushBot;

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

    public static boolean attack() {
        if (!V.rc.isActionReady()) {
            return false;
        }
        try {
            RobotInfo[] enemies;
            //        if (V.rc.isMovementReady()) {
            //            enemies = V.rc.senseNearbyRobots(11, V.team.opponent());
            //            Arrays.sort(enemies, (a, b) -> a.getHealth() == b.getHealth() ? b.getAttackLevel() - a.getAttackLevel() : a.getHealth() - b.getHealth());
            //            for (RobotInfo enemy: enemies) {
            //                MapLocation loc = enemy.getLocation();
            //                if (V.rc.canAttack(loc)) {
            //                    V.lastAttackTimestamp = V.round;
            //                    V.rc.attack(loc);
            //                    return true;
            //                }
            //                if (V.rc.isMovementReady()) {
            //                    Direction dir = V.rc.getLocation().directionTo(loc);
            //                    for (Direction choice: new Direction[]{dir, dir.rotateLeft(), dir.rotateRight()}) {
            //                        if (V.rc.adjacentLocation(choice).isWithinDistanceSquared(loc, 4) && V.rc.canMove(choice)) {
            //                            V.rc.move(choice);
            //                            if (V.rc.canAttack(loc)) {
            //                                V.lastAttackTimestamp = V.round;
            //                                V.rc.attack(loc);
            //                            }
            //                            return true;
            //                        }
            //                    }
            //                }
            //            }
            //        } else {
            enemies = V.rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, V.team.opponent());
            /*Arrays.sort(enemies, (a, b) -> {
                int aLevel = 2 * a.getAttackLevel() * a.getAttackLevel() + a.getHealLevel() * 4 + a.getBuildLevel();
                int bLevel = 2 * b.getAttackLevel() * b.getAttackLevel() + b.getHealLevel() * 4 + b.getBuildLevel();
                if (aLevel == bLevel) {
                    if (a.getHealth() == b.getHealth()) {
                        return a.getID() - b.getID();
                    }
                    return a.getHealth() - b.getHealth();
                }
                return bLevel - aLevel;
            });*/
            if (enemies.length <= 0) return false;
            RobotInfo bestTarget = enemies[0], a, b;
            for (RobotInfo enemy : enemies) {
                a = bestTarget;
                b = enemy;
                boolean newBest = false;
                int aLevel = 2 * a.getAttackLevel() * a.getAttackLevel() + a.getHealLevel() * 4 + a.getBuildLevel();
                int bLevel = 2 * b.getAttackLevel() * b.getAttackLevel() + b.getHealLevel() * 4 + b.getBuildLevel();
                if (aLevel == bLevel) {
                    if (a.getHealth() == b.getHealth()) {
                        newBest = a.getID() > b.getID();
                    } else {
                        newBest = a.getHealth() > b.getHealth();
                    }
                } else {
                    newBest = bLevel > aLevel;
                }
                if (newBest) {
                    bestTarget = enemy;
                }
            }

            MapLocation loc = bestTarget.getLocation();
            if (V.rc.canAttack(loc)) {
                V.lastAttackTimestamp = V.round;
                attackUtil(loc);
                return true;
            }
            //        }
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
        return false;
    }
}
