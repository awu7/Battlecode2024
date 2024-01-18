package RushBot;

import battlecode.common.*;

import java.lang.System;
import java.util.*;

public strictfp class RobotPlayer {
    public static void run(RobotController _rc) throws GameActionException {
        V.rc = _rc;
        RobotUtils.init();
        while (true) {
            try {
                V.round = V.rc.getRoundNum();
                if (!V.rc.isSpawned()) {
                    Spawning.attemptSpawn();
                    if (!V.rc.isSpawned()) {
                        continue;
                    }
                }
                if (V.round == 1) {
                    IDCompression.writeID();
                } else if (V.round == 2) {
                    IDCompression.readIDs();
                    if (V.selfIdx == 49) {
                        for (int i = 0; i < 50; ++i) {
                            V.rc.writeSharedArray(i, 0);
                        }
                    }
                    int f = 0;
                    for (MapLocation m : V.rc.getAllySpawnLocations()){
                        int adjCount = 0;
                        for(MapLocation m2 : V.rc.getAllySpawnLocations()) {
                            if(Math.abs(m.x - m2.x) <= 1 && Math.abs(m.y - m2.y) <= 1) adjCount++;
                        }
                        if (adjCount == 9){
                            V.spawnCentres[f] = m;
                            f++;
                        }
                    }
                    for(int i = 40; i < 43; i++) {
                        if(V.ids[i] + 9999 == V.rc.getID()) V.home = V.spawnCentres[i - 40];
                    }
                    for(int i = 37; i < 40; i++) {
                        if(V.ids[i] + 9999 == V.rc.getID()) V.isBuilder = true;
                    }
                }
                if (!Bfs.precomp()) {
                    // run out of bytecode
                    continue;
                }
                RobotUtils.buyGlobal();
                if(V.rc.onTheMap(V.home)) {
                    if(!V.rc.getLocation().equals(V.home)) {
                        BugNav.moveBetter(V.home);
                        V.rc.setIndicatorLine(V.rc.getLocation(), V.home, 0, 0, 255);
                    }
                    if(V.rc.getLocation().equals(V.home)) Building.trapSpawn();
                } else if (V.round <= 150) {
                    if(V.isBuilder) {
                        Building.farmBuildXp(6);
                        Building.farmBuildXp(6);
                        Building.farmBuildXp(6);
                        Building.farmBuildXp(6);
                    }
                    CrumbFinding.randomWalk();
                    continue;
                }
                Movement.AllMovements();
                Capture.pickupFlag(true);
                Healing.healFlagBearer();
                Attacking.attack();
                if (V.round > 1900) Building.farmBuildXp(3);
                Healing.heal();
                if(V.rc.getCrumbs() > 5000) Building.buildTraps();
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
