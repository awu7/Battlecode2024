package OldRushBot;

import battlecode.common.*;

import java.util.Arrays;

public class Attacking {
    public static boolean attackFlag() throws GameActionException {
        if (!V.rc.isActionReady()) {
            return false;
        }
        for (RobotInfo enemy: V.rc.senseNearbyRobots(11, V.team.opponent())) {
            if (enemy.hasFlag()) {
                MapLocation loc = enemy.getLocation();
                if (V.rc.canAttack(loc)) {
                    V.rc.attack(loc);
                    return true;
                }
                if (V.rc.isMovementReady()) {
                    Direction dir = V.rc.getLocation().directionTo(loc);
                    for (Direction choice: new Direction[]{dir, dir.rotateLeft(), dir.rotateRight()}) {
                        if (V.rc.adjacentLocation(choice).isWithinDistanceSquared(loc, 4) && V.rc.canMove(choice)) {
                            V.rc.move(choice);
                            V.rc.attack(loc);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean attack() throws GameActionException {
        if (!V.rc.isActionReady()) {
            return false;
        }
        RobotInfo[] enemies;
        if (V.rc.isMovementReady()) {
            enemies = V.rc.senseNearbyRobots(11, V.team.opponent());
            Arrays.sort(enemies, (a, b) -> {
                return a.getHealth() == b.getHealth() ? b.getAttackLevel() - a.getAttackLevel() : a.getHealth() - b.getHealth();
            });
            for (RobotInfo enemy: enemies) {
                MapLocation loc = enemy.getLocation();
                if (V.rc.canAttack(loc)) {
                    V.rc.attack(loc);
                    return true;
                }
                if (V.rc.isMovementReady()) {
                    Direction dir = V.rc.getLocation().directionTo(loc);
                    for (Direction choice: new Direction[]{dir, dir.rotateLeft(), dir.rotateRight()}) {
                        if (V.rc.adjacentLocation(choice).isWithinDistanceSquared(loc, 4) && V.rc.canMove(choice)) {
                            V.rc.move(choice);
                            V.rc.attack(loc);
                            return true;
                        }
                    }
                }
            }
        } else {
            enemies = V.rc.senseNearbyRobots(4, V.team.opponent());
            Arrays.sort(enemies, (a, b) -> {
                return a.getHealth() == b.getHealth() ? b.getAttackLevel() - a.getAttackLevel() : a.getHealth() - b.getHealth();
            });
            for (RobotInfo enemy: enemies) {
                MapLocation loc = enemy.getLocation();
                if (V.rc.canAttack(loc)) {
                    V.rc.attack(loc);
                    return true;
                }
            }
        }
        return false;
    }
}
