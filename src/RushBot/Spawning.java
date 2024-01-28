package RushBot;

import battlecode.common.*;

public class Spawning {
    public static void attemptSpawn() throws GameActionException {
        MapLocation[] spawnLocs = V.spawns;
        RobotUtils.shuffle(spawnLocs);
        if (V.rc.onTheMap(V.home)) {
            if (V.rc.canSpawn(V.home)) {
                V.rc.spawn(V.home);
                return;
            } else {
                for (Direction dir : V.directions) {
                    if (V.rc.canSpawn(V.home.add(dir))) {
                        V.rc.spawn(V.home.add(dir));
                        return;
                    }
                }
                return;
            }
        }
        spawnLocsLoop:
        for (MapLocation loc : spawnLocs) {
            for (MapLocation centre : V.spawnCentres) {
                if (loc.equals(centre)) {
                    continue spawnLocsLoop;
                }
            }
            if (V.rc.canSpawn(loc)) {
                V.rc.spawn(loc);
                V.swarmTarget = new MapLocation(-1, -1);
                break;
            }
        }
    }
}
