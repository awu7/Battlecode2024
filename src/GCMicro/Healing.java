package GCMicro;

import battlecode.common.*;

public class Healing {
    static void healFlagBearer() throws GameActionException {
        RobotInfo[] nearby = V.rc.senseNearbyRobots();
        for (RobotInfo robot: nearby) {
            if (robot.hasFlag() && robot.getTeam() == V.rc.getTeam()) {
                MapLocation loc = robot.getLocation();
                if (V.rc.canHeal(loc)) {
                    V.rc.heal(loc);
                    return;
                }
            }
        }
    }
    static void heal() throws GameActionException {
        // Also prioritise flag carriers when healing
        RobotInfo[] nearbyAllyRobots = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
        RobotInfo healTarget = V.rc.senseRobot(V.rc.getID());
        for (RobotInfo ally : nearbyAllyRobots) {
            if(V.rc.canHeal(ally.getLocation())
                    && ally.health < healTarget.health) {
                healTarget = ally;
            }
        }
        if(V.rc.canHeal(healTarget.getLocation())) {
            V.rc.heal(healTarget.getLocation());
        }
    }

}
