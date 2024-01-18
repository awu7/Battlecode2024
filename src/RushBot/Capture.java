package RushBot;

import battlecode.common.*;

public class Capture {
    public static void pickupFlagUtil(FlagInfo flag) throws GameActionException {
        MapLocation flagLoc = flag.getLocation();
        if (!V.rc.canPickupFlag(flagLoc)) {
            return;
        }
        if (flagLoc.equals(V.rc.getLocation())) {
            V.rc.pickupFlag(flagLoc);
            return;
        }
        int i = V.spawnBfs != null ? V.spawnBfs[flagLoc.x][flagLoc.y] : 0;
        if (i > 0) {
            Direction dir = V.directions[i - 1];
            for (Direction choice: new Direction[]{dir, dir.rotateLeft(), dir.rotateRight()}) {
                MapLocation next = flagLoc.add(choice);
                if (next.equals(V.rc.getLocation())) {
                    V.rc.pickupFlag(flagLoc);
                    BugNav.readStack();
                    return;
                }
                RobotInfo friend = V.rc.senseRobotAtLocation(next);
                if (friend != null && friend.getTeam() == V.rc.getTeam()) {
                    RobotUtils.debug("Letting friend pickup");
                    return;
                }
            }
        }
        V.rc.pickupFlag(flagLoc);
        BugNav.readStack();
    }

    public static void pickupFlag(boolean allowCurrentCell) throws GameActionException {
        if(V.rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
            for(FlagInfo f : V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent())) {
                pickupFlagUtil(f);
            }
        }
    }

    /**
     * Helper function to check if a friend can pick up a flag dropped in a direction.
     * @param dir the direction to drop the flag.
     * @return
     */
    public static boolean canFriendPickup(Direction dir) throws GameActionException {
        MapLocation loc = V.rc.getLocation().add(dir);
        RobotInfo robot = V.rc.senseRobotAtLocation(loc);
        if (robot != null && robot.getTeam() == V.rc.getTeam()) {
            return true;
        }
        robot = V.rc.senseRobotAtLocation(loc.add(dir));
        if (robot != null && robot.getTeam() == V.rc.getTeam()) {
            return true;
        }
        robot = V.rc.senseRobotAtLocation(loc.add(dir.rotateRight()));
        if (robot != null && robot.getTeam() == V.rc.getTeam()) {
            return true;
        }
        robot = V.rc.senseRobotAtLocation(loc.add(dir.rotateLeft()));
        if (robot != null && robot.getTeam() == V.rc.getTeam()) {
            return true;
        }
        return false;
    }
}
