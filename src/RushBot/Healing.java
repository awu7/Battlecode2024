package RushBot;

import battlecode.common.*;

import java.util.Arrays;

public class Healing {
    public static void healFlag() throws GameActionException {
        RobotInfo[] friends = V.rc.senseNearbyRobots(4, V.team);
        for (RobotInfo friend: friends) {
            if (friend.hasFlag()) {
                MapLocation loc = friend.getLocation();
                if (V.rc.canHeal(loc)) {
                    V.rc.heal(loc);
                    return;
                }
            }
        }
    }

    public static void heal() throws GameActionException {
//        RobotInfo[] enemies = V.rc.senseNearbyRobots(-1, V.team.opponent());
//        if (enemies.length > 4) {
//            return;
//        }
//        int hp = 0;
//        for (RobotInfo enemy: enemies) {
//            hp += enemy.getHealth();
//        }
//        if (hp > 1000) {
//            return;
//        }
        RobotInfo[] friends = V.rc.senseNearbyRobots(4, V.team);
        Arrays.sort(friends, (a, b) -> a.getAttackLevel() == b.getAttackLevel() ? a.getHealth() - b.getHealth() : b.getAttackLevel() - a.getAttackLevel());
        for (RobotInfo friend: friends) {
            MapLocation loc = friend.getLocation();
            if (V.rc.canHeal(loc)) {
                V.rc.heal(loc);
                return;
            }
        }
    }
}
