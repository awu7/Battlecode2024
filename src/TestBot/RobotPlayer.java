package TestBot;

import battlecode.common.*;

import java.lang.System;

@SuppressWarnings("unused")
public strictfp class RobotPlayer {
    public static void run(RobotController _rc) {
        V.rc = _rc;
        RobotUtils.init();
        main: while (true) {
            try {
                V.round = V.rc.getRoundNum();
//                if (V.round > 250) V.rc.resign(); // REMOVE THIS WHEN NOT TESTING
                if (!V.rc.isSpawned() && V.round != Consts.BFS_ROUND + 2 && V.round != Consts.BFS_ROUND + 3 && V.round >= 3) {
                    // Wait until 3rd round because we don't know if we are the flag sitter until 3rd round
                    Spawning.attemptSpawn();
                }
                if (V.round == 1) {
                    IDCompression.writeID();
                    continue;
                } else if (V.round == 2) {
                    IDCompression.init();
                    IDCompression.readIDs();
                    if (V.selfIdx == 49) {
                        UnrolledUtils.clearSharedArray();
                    }
                    int f = 0;
                    for (MapLocation m : V.spawns) {
                        int adjCount = 0;
                        for(MapLocation m2 : V.spawns) {
                            if (m.isAdjacentTo(m2)) adjCount++;
                        }
                        if (adjCount == 9) {
                            V.spawnCentres[f++] = m;
                        }
                    }
                    if (V.selfIdx >= 37 && V.selfIdx <= 39) {
                        V.isBuilder = true;
                    }
                    if (V.selfIdx >= Consts.LOWEST_FLAG_SITTER && V.selfIdx <= Consts.HIGHEST_FLAG_SITTER) {
                        V.home = V.spawnCentres[V.selfIdx - Consts.LOWEST_FLAG_SITTER];
                        V.flagHome = V.home;
                        V.rc.writeSharedArray(V.selfIdx-Consts.LOWEST_FLAG_SITTER+Consts.LOWEST_FS_COMMS_IDX, Comms.encode(V.flagHome));
                    }
                    continue;
                }
                //if (V.flagHome != null) RobotUtils.debug("FS");
                if (!Bfs.precomp()) {
                    // run out of bytecode
                    continue;
                }
                Upgrades.buyUpgrade();
                if (!V.rc.isSpawned()) {
                    continue;
                }
                RobotUtils.startRound();
                if (V.flagHome != null) {
                    if (V.round == 50) {
                        V.wallWeights = new int[V.width][];
                        for (int x = V.width; --x >= 0;) {
                            UnrolledUtils.fill(V.wallWeights[x] = new int[V.height], 0);
                        }
                    }
                    if (V.round == 75) {
                        V.flagWeights = new int[V.width][];
                        for (int x = V.width; --x >= 0;) {
                            UnrolledUtils.fill(V.flagWeights[x] = new int[V.height], -100);
                        }
                    }
                    if (V.round <= Consts.SYMMETRY_ONE + 4 || V.round >= GameConstants.SETUP_ROUNDS-1) {
                        // By now we should have the flag in a secure location
                        // So we just need to walk to the flag or put traps on it if we are already there
                        if (!V.rc.getLocation().equals(V.flagHome)) {
                            BugNav.moveBetter(V.flagHome);
                            V.rc.setIndicatorLine(V.rc.getLocation(), V.flagHome, 0, 0, 255);
                        }
                        if (V.rc.getLocation().equals(V.flagHome) && V.round >= GameConstants.SETUP_ROUNDS-10) Building.trapSpawn();
                    } else {
                        Movement.SetupFlags();
                        continue;
                    }
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
                for (Direction dir: Direction.cardinalDirections()) {
                    MapLocation loc = V.rc.adjacentLocation(dir);
                    if (V.rc.onTheMap(loc) && V.rc.senseMapInfo(loc).isDam()) {
                        continue main;
                    }
                }
                Capture.capture();
                if (Attacking.attackFlag()) {
                    Attacking.attackFlag();
                }
//                if(V.isBuilder) {
//                    Building.buildTraps();
//                    Building.farmBuildXp(4);
//                    Building.farmBuildXp(4);
//                    Building.farmBuildXp(4);
//                    Building.farmBuildXp(4);
//                }
                if (V.flagHome == null) {
                    Movement.AllMovements();
                }
                if (Attacking.attack()) {
                    Attacking.attack();
                }
                if (V.round >= 201) {
                    Building.buildTraps();
                }
                if (V.round > 1900) Building.farmBuildXp(3);
//                if(V.rc.getCrumbs() > 5000) Building.buildTraps();
                Healing.healFlag();
                boolean nearEnemies = V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent()).length > 0;
                if (!nearEnemies || (V.round - V.lastAttackTimestamp) > 4) {
                    Healing.heal();
                }
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
