package RushBot;

import battlecode.common.*;

import java.lang.System;

@SuppressWarnings("unused")
public strictfp class RobotPlayer {
    public static void run(RobotController _rc) {
        V.rc = _rc;
        RobotUtils.init();
        while (true) {
            try {
                V.round = V.rc.getRoundNum();
                if (!V.rc.isSpawned() && V.round != Consts.BFS_ROUND + 2 && V.round != Consts.BFS_ROUND + 3) {
                    Spawning.attemptSpawn();
                }
                if (V.round == 1) {
                    IDCompression.writeID();
                    RobotUtils.shuffle(V.shuffledDirections);
                    for (Direction dir : V.shuffledDirections) {
                        if (V.rc.canMove(dir)) {
                            if (!V.rc.senseMapInfo(V.rc.getLocation().add(dir)).isSpawnZone()) {
                                V.rc.move(dir);
                            }
                        }
                    }
                    continue;
                } else if (V.round == 2) {
                    IDCompression.init();
                    IDCompression.readIDs();
                    if (V.selfIdx == 49) {
                        UnrolledUtils.clearSharedArray();
                    }
                    int f = 0;
                    for (MapLocation m : V.rc.getAllySpawnLocations()) {
                        int adjCount = 0;
                        for(MapLocation m2 : V.rc.getAllySpawnLocations()) {
                            if (m.isAdjacentTo(m2)) adjCount++;
                        }
                        if (adjCount == 9) {
                            V.spawnCentres[f++] = m;
                        }
                    }
                    if (V.selfIdx >= 37 && V.selfIdx <= 39) {
                        V.isBuilder = true;
                    }
                    if (V.selfIdx >= 40 && V.selfIdx <= 42) {
                        V.home = V.spawnCentres[V.selfIdx - 40];
                    }
                    continue;
                }
                if (!Bfs.precomp()) {
                    // run out of bytecode
                    continue;
                }
                RobotUtils.buyGlobal();
                if (!V.rc.isSpawned()) {
                    continue;
                }
                if(V.rc.onTheMap(V.home)) {
                    if(!V.rc.getLocation().equals(V.home)) {
                        BugNav.moveBetter(V.home);
                        V.rc.setIndicatorLine(V.rc.getLocation(), V.home, 0, 0, 255);
                    }
                    if(V.rc.getLocation().equals(V.home)) Building.trapSpawn();
                } else if (V.round <= 150) {
//                    if(V.isBuilder) {
//                        Building.farmBuildXp(6);
//                        Building.farmBuildXp(6);
//                        Building.farmBuildXp(6);
//                        Building.farmBuildXp(6);
//                    }
                    CrumbFinding.randomWalk();
                    continue;
                }
                Capture.capture();
                if (Attacking.attackFlag()) {
                    Attacking.attackFlag();
                }
                if (V.round >= 200) {
                    Building.buildTraps();
                }
//                if(V.isBuilder) {
//                    Building.buildTraps();
//                    Building.farmBuildXp(4);
//                    Building.farmBuildXp(4);
//                    Building.farmBuildXp(4);
//                    Building.farmBuildXp(4);
//                }
                if (Attacking.attack()) {
                    Attacking.attack();
                }
                Movement.AllMovements();
                if (Attacking.attack()) {
                    Attacking.attack();
                }
                if (V.round > 1900) Building.farmBuildXp(3);
//                if(V.rc.getCrumbs() > 5000) Building.buildTraps();
                Healing.healFlag();
                Healing.heal();
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                RobotUtils.endRound();
                Clock.yield();
            }
        }
    }
}
