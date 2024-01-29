package RushBot;

import battlecode.common.*;

import java.util.Arrays;

public class Healing {
    public static boolean healed = false;

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

    public static class HealTarget {
        MapLocation loc;
        int skill, health;

        HealTarget() {
            loc = V.rc.getLocation();
            skill = V.rc.getLevel(SkillType.ATTACK);
            health = V.rc.getHealth();
        }

        HealTarget(RobotInfo info) {
            loc = info.getLocation();
            skill = info.getAttackLevel();
            health = info.getHealth();
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
            HealTarget[] targets = new HealTarget[friends.length + 1];
            for (int i = friends.length; --i >= 0;) {
                targets[i] = new HealTarget(friends[i]);
            }
            targets[friends.length] = new HealTarget();
            Arrays.sort(targets, (a, b) -> {
                if (a.skill == b.skill) {
                    return a.health - b.health;
                }
                return b.skill - a.skill;
            });
            for (HealTarget target: targets) {
                if (V.rc.canHeal(target.loc)) {
//                    System.out.println("Healing " + friend.getID() + " with attack level " + friend.getAttackLevel() + ", health " + friend.getHealth() + ", own health " + V.rc.getHealth());
                    healUtil(target.loc);
                    return true;
                } else {
//                    System.out.println("Can't heal " + friend.getID());
                }
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
        return false;
    }
}
