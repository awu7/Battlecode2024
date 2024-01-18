package OldRushBot;

import battlecode.common.*;

public class Spawning {
    public static void attemptSpawn() throws GameActionException {
        MapLocation[] spawnLocs = V.rc.getAllySpawnLocations();
        // Arrays.sort(spawnLocs, RobotUtils.closestComp(centre));
        for(int i = spawnLocs.length - 1; i > 0; i--) {
            int j = V.rng.nextInt(i);
            MapLocation tmp = spawnLocs[i];
            spawnLocs[i] = spawnLocs[j];
            spawnLocs[j] = tmp;
        }
        for (MapLocation loc : spawnLocs) {
            if (V.rc.canSpawn(loc)) {
                V.rc.spawn(loc);
                V.swarmTarget = new MapLocation(-1, -1);
                if (V.round == 1) {
                    RobotUtils.shuffle(V.shuffledDirections);
                    for (Direction dir : V.shuffledDirections) {
                        if (V.rc.canMove(dir)) {
                            if (!V.rc.senseMapInfo(V.rc.getLocation().add(dir)).isSpawnZone()) {
                                V.rc.move(dir);
                            }
                        }
                    }
                }
                break;
            }
        }
    }
}
