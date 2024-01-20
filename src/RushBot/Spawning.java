package RushBot;

import battlecode.common.*;

public class Spawning {
    public static void attemptSpawn() throws GameActionException {
        MapLocation[] spawnLocs = V.rc.getAllySpawnLocations();
        RobotUtils.shuffle(spawnLocs);
        for (MapLocation loc : spawnLocs) {
            if (V.rc.canSpawn(loc)) {
                V.rc.spawn(loc);
                V.swarmTarget = new MapLocation(-1, -1);
                break;
            }
        }
    }
}
