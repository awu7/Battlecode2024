package HeuristicBot;

import battlecode.common.*;

import java.util.Arrays;

public class Healing {
    public static double[] healing = {80, 82.4, 84, 85.6, 88, 92, 100};
    public static double[] cd = {30, 28.5, 27, 25.5, 25.5, 25.5, 22.5};
    public static double[] hps;
    public static boolean healed = false;

    public static void init() {
        hps = new double[7];
        updateHps();
    }

    public static void updateHps() {
        hps[0] = healing[0] * 10 / cd[0];
        hps[1] = healing[1] * 10 / cd[1];
        hps[2] = healing[2] * 10 / cd[2];
        hps[3] = healing[3] * 10 / cd[3];
        hps[4] = healing[4] * 10 / cd[4];
        hps[5] = healing[5] * 10 / cd[5];
        hps[6] = healing[6] * 10 / cd[6];
    }

    public static void updateHealing() {
        healing[0] += 50;
        healing[1] += 50;
        healing[2] += 50;
        healing[3] += 50;
        healing[4] += 50;
        healing[5] += 50;
        healing[6] += 50;
        updateHps();
    }

    public static void healUtil(MapLocation loc) {
        try {
            V.rc.heal(loc);
            V.history.append('H');
            healed = true;
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
    }

    public static void healFlag() throws GameActionException {
        RobotInfo[] friends = V.rc.senseNearbyRobots(4, V.team);
        for (RobotInfo friend: friends) {
            if (friend.hasFlag()) {
                MapLocation loc = friend.getLocation();
                if (V.rc.canHeal(loc)) {
                    healUtil(loc);
                    return;
                }
            }
        }
    }

    public static boolean heal(boolean enforced) {
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
                    if (friend.getHealth() + V.rc.getHealAmount() > 1000) {
                        return false;
                    }
                    if (!enforced) {
                        if (friend.getHealth() > 450) {
                            return false;
                        }
                    }
                    healUtil(loc);
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
