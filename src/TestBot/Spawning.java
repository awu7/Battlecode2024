package TestBot;

import battlecode.common.*;

public class Spawning {
    public static boolean spawn(MapLocation loc) {
        try {
            V.rc.spawn(loc);
            RobotUtils.updateRobots();
            return true;
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
        return false;
    }

    public static void attemptSpawn() throws GameActionException {
        MapLocation[] spawnLocs = V.spawns;
        RobotUtils.shuffle(spawnLocs);
        if (V.home != null) {
            if (V.rc.canSpawn(V.home)) {
                if (spawn(V.home)) {
                    return;
                }
            } else {
                for (Direction dir : V.directions) {
                    if (V.rc.canSpawn(V.home.add(dir))) {
                        if (spawn(V.home.add(dir))) {
                            return;
                        }
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
                if (spawn(loc)) {
                    V.swarmTarget = new MapLocation(-1, -1);
                    break;
                }
            }
        }
    }
}
