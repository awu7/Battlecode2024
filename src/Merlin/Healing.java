package Merlin;

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
        RobotInfo[] friends = V.rc.senseNearbyRobots(4, V.rc.getTeam());
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

    private static class HealTarget {
        MapLocation loc;
        boolean canHeal;
        int level;
        int health;
        int id;

        HealTarget(RobotInfo robotInfo) {
            loc = robotInfo.getLocation();
            canHeal = V.rc.canHeal(loc);
            level = robotInfo.getAttackLevel();
            health = robotInfo.getHealth();
            id = robotInfo.getID();
        }

        public boolean isBetter(HealTarget h) {
            if (canHeal != h.canHeal) {
                return canHeal;
            }
            if (level != h.level) {
                return level > h.level;
            }
            if (health != h.health) {
                return health < h.health;
            }
            return id < h.id;
        }
    }

    public static boolean heal(boolean enforced) {
        if (!V.rc.isActionReady()) {
            return false;
        }
        if (V.selfIdx % 2 == 0 && V.rc.getLevel(SkillType.ATTACK) <= 3 && V.rc.getLevel(SkillType.HEAL) == 3) {
            return false;
        }
        try {
            RobotInfo[] friends = V.rc.senseNearbyRobots(4, V.team);
            int n = friends.length;
            if (n == 0) {
                return false;
            }
            HealTarget bestTarget = new HealTarget(friends[n - 1]);
            for (int i = n - 1; --i >= 0;) {
                HealTarget h = new HealTarget(friends[i]);
                if (h.isBetter(bestTarget)) {
                    bestTarget = h;
                }
            }
            if (V.rc.canHeal(bestTarget.loc)) {
                if (bestTarget.health + V.rc.getHealAmount() > 1000) {
                    return false;
                }
                if (!enforced) {
                    if (bestTarget.health > 450) {
                        return false;
                    }
                }
                healUtil(bestTarget.loc);
                return true;
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
        return false;
    }
}
