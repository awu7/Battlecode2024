package OldRushBot;

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

    public static boolean heal() {
        if (!V.rc.isActionReady()) {
            return false;
        }
        if (V.rc.getLevel(SkillType.ATTACK) <= 3 && V.rc.getLevel(SkillType.HEAL) == 3) {
            return false;
        }
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
        try {
            RobotInfo[] friends = V.rc.senseNearbyRobots(4, V.team);
            Arrays.sort(friends, (a, b) -> a.getAttackLevel() == b.getAttackLevel() ? a.getHealth() - b.getHealth() : b.getAttackLevel() - a.getAttackLevel());
            for (RobotInfo friend : friends) {
                MapLocation loc = friend.getLocation();
                if (V.rc.canHeal(loc)) {
                    V.rc.heal(loc);
                    return true;
                }
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
        return false;
    }
}
