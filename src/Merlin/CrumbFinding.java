package Merlin;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CrumbFinding {
    public static void randomWalk() throws GameActionException {
        V.nearbyAllies = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
        MapLocation nextLoc;
        MapLocation[] rawCrumbs = V.rc.senseNearbyCrumbs(-1);
        List<MapLocation> validCrumbs = new ArrayList<MapLocation>();
        for (MapLocation crumb : rawCrumbs) {
            if (!V.rc.senseMapInfo(crumb).isWall()) validCrumbs.add(crumb);
        }
        MapLocation[] crumbs = validCrumbs.toArray(new MapLocation[0]);
        if(crumbs.length > 0) {
            // sort crumbs by distance
            Arrays.sort(crumbs, RobotUtils.closestComp());
            nextLoc = crumbs[0];
        } else {
            if(V.movesLeft > 0 && !V.rc.getLocation().equals(V.target)) {
                V.movesLeft--;
            } else {
                V.target = new MapLocation(StrictMath.max(0, StrictMath.min(V.width - 1, V.rc.getLocation().x + V.rng.nextInt(21) - 10)),
                        StrictMath.max(0, StrictMath.min(V.height - 1, V.rc.getLocation().y + V.rng.nextInt(21) - 10)));
                V.movesLeft = 7;
            }
            nextLoc = V.target;
        }
        BugNav.moveBetter(nextLoc);
    }
}
