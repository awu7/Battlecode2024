package OldRushBot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

public class Building {
    public static void farmBuildXp(int level) throws GameActionException {
        if(V.rc.getLevel(SkillType.BUILD) < level) {
            for(Direction d : V.directions) {
                if((V.rc.adjacentLocation(d).x % 2) == (V.rc.adjacentLocation(d).y % 2)) continue;
                if(V.rc.canDig(V.rc.getLocation().add(d))) V.rc.dig(V.rc.getLocation().add(d));
            }
        }
    }

    public static void tryBuildTrap(MapLocation loc, RobotInfo[] visibleEnemies, int nearbyTraps, boolean ok) throws GameActionException {
        TrapType chosenTrap = V.rng.nextInt((visibleEnemies.length<=2)?5:2) == 0 ? TrapType.STUN : TrapType.EXPLOSIVE;
        boolean adjTrap = false;
        boolean veryCloseTrap = false;
        if(!V.rc.onTheMap(loc)) return;
        for(MapInfo m : V.rc.senseNearbyMapInfos(loc, 5)) {
            if(m.getTrapType() != TrapType.NONE) {
                adjTrap = true;
                if (m.getMapLocation().distanceSquaredTo(loc) <= 2) {
                    veryCloseTrap = true;
                }
            }
        }
        // CR in this context is chance reciprocal
        int wallCR = (ok)?0:100; // Instead of outright cancel, make it a weighting
        int nearbyTrapCR = 50+nearbyTraps*100;
        //int dissuadeEdgeCR = 71 - 10 * StrictMath.min(RobotUtils.distFromEdge(V.rc.getLocation()), 7);
        int nearbyEnemiesCR = StrictMath.max(100 - (50 * visibleEnemies.length), 1);
        int chanceReciprocal = StrictMath.min(nearbyTrapCR, nearbyEnemiesCR) + wallCR;// + dissuadeEdgeCR;
        if((!veryCloseTrap || chosenTrap == TrapType.EXPLOSIVE) && (!adjTrap || V.rc.getCrumbs() > 5000 || nearbyEnemiesCR <= 2) && V.rc.canBuild(chosenTrap, loc) && V.rng.nextInt(chanceReciprocal) == 0) {
            V.rc.build(chosenTrap, loc);
        }
    }

    public static void buildTraps() throws GameActionException {
        boolean ok = true;
        MapInfo[] mapInfos = V.rc.senseNearbyMapInfos(3);
        for(MapInfo m : mapInfos) {
            if(m.isWall()) {
                ok = false;
                break;
            }
        }
        for(MapInfo m : V.rc.senseNearbyMapInfos(2)) {
            if(m.isSpawnZone() && m.getSpawnZoneTeamObject() != V.rc.getTeam()) {
                ok = true;
            }
        }
        //if(!ok) return; // turned into weighting, see below
        if(V.rc.getCrumbs() >= 250 && V.rc.getRoundNum() >= 180) {
            RobotInfo[] visibleEnemies = V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent());
            if(visibleEnemies.length == 0) return;
            // Calculate number of nearby traps
            int nearbyTraps = 0;
            for (MapInfo mi : mapInfos) {
                if (mi.getTrapType() != TrapType.NONE) {
                    ++nearbyTraps;
                }
            }
            for(Direction d : Direction.values()) {
                if(V.rc.onTheMap(V.rc.adjacentLocation(d)) && V.rc.senseMapInfo(V.rc.adjacentLocation(d)).isWater()) continue;
                tryBuildTrap(V.rc.adjacentLocation(d), visibleEnemies, nearbyTraps, ok);
            }
            for(Direction d : Direction.values()) {
                tryBuildTrap(V.rc.adjacentLocation(d), visibleEnemies, nearbyTraps, ok);
            }
        }
    }

    public static void trapSpawn() throws GameActionException {
        for (int i=0; i<3; i++){
            if (V.rc.canBuild(TrapType.STUN, V.rc.getLocation().add(V.trapDirs[i]))) {
                V.rc.build(TrapType.STUN, V.rc.getLocation().add(V.trapDirs[i]));
            }
        }
    }

    public static void updateStuns() throws GameActionException {
        List<MapLocation> currStuns = new ArrayList<MapLocation>();
        MapInfo[] mapInfos = V.rc.senseNearbyMapInfos(-1);
        for (MapInfo mi : mapInfos) {
            if (mi.getTrapType() == TrapType.STUN || mi.getTrapType() == TrapType.EXPLOSIVE) {
                currStuns.add(mi.getMapLocation());
            }
        }
        for (MapLocation ml : V.prevStuns) {
            if (ml.distanceSquaredTo(V.rc.getLocation()) < 20) {
                boolean triggered = true;
                for (MapLocation ml2 : currStuns) {
                    if (ml2.equals(ml)) {
                        triggered = false;
                        break;
                    }
                }
                if (triggered) {
                    V.activeStuns.add(new ActiveStun(ml));
                }
            }
        }
        V.prevStuns = currStuns;
        RobotUtils.debug("currStuns: " + currStuns.size());
        List<ActiveStun> newActiveStuns = new ArrayList<ActiveStun>();
        for (ActiveStun stun : V.activeStuns) {
            if (stun.updateRound()) {
                newActiveStuns.add(stun);
            }
        }
        V.activeStuns = newActiveStuns;
    }
}
