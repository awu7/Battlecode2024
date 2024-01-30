package Merlin;

import battlecode.common.*;

import java.util.Arrays;

public class Targetting {
    public static void broadcastSwarmTarget(MapLocation loc) throws GameActionException {
        V.rc.writeSharedArray(0, V.rc.getID());
        V.rc.writeSharedArray(1, (loc.x << 6) | loc.y);
    }

    public static MapLocation findTarget() throws GameActionException {
        int swarmLeader = V.rc.readSharedArray(0);
        if(swarmLeader == V.rc.getID()) {
            V.rc.writeSharedArray(0, 0);
        }
        if(V.rc.hasFlag()) return RobotUtils.closest(V.spawns);
        FlagInfo[] friendlyFlags = V.rc.senseNearbyFlags(-1, V.rc.getTeam());
        if(friendlyFlags.length > 0 && V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent()).length > 0) {
            for(FlagInfo f : friendlyFlags) {
                if(V.rc.senseNearbyRobots(f.getLocation(), 0, V.rc.getTeam().opponent()).length > 0) {
                    broadcastSwarmTarget(f.getLocation());
                    return f.getLocation();
                }
            }
        }
        FlagInfo[] possibleFlags = V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent());
        MapLocation[] flagLocs = new MapLocation[possibleFlags.length];
        for(int i = 0; i < possibleFlags.length; i++) {
            if(!possibleFlags[i].isPickedUp()) flagLocs[i] = possibleFlags[i].getLocation();
            else flagLocs[i] = new MapLocation(-1, -1);
        }
        MapLocation closestFlag = RobotUtils.closest(flagLocs);
        if(V.rc.onTheMap(closestFlag)) return closestFlag;
        if (V.lastFlag) {
            MapLocation[] broadcasts = V.rc.senseBroadcastFlagLocations();
            if (broadcasts.length > 0) {
                return broadcasts[0];
            }
        }
        RobotInfo[] enemies = V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent());
        MapLocation[] enemyLocs = new MapLocation[enemies.length];
        for(int i = 0; i < enemies.length; i++) {
            enemyLocs[i] = enemies[i].getLocation();
        }
        if(enemies.length >= 1) {
            return RobotUtils.closest(enemyLocs);
        }
        if(V.swarmEnd < V.rc.getRoundNum()) V.swarmTarget = new MapLocation(-1, -1);
        if(V.rc.onTheMap(V.swarmTarget)) return V.swarmTarget;
        if(swarmLeader != 0) {
            int encodedLoc = V.rc.readSharedArray(1);
            MapLocation newSwarmTarget = new MapLocation(encodedLoc >> 6, encodedLoc & ((1 << 6) - 1));
            if(Math.sqrt(V.rc.getLocation().distanceSquaredTo(V.swarmTarget)) < StrictMath.max(V.height, V.width) * 0.5) {
                V.swarmTarget = newSwarmTarget;
                V.swarmEnd = V.rc.getRoundNum() + StrictMath.max(V.height, V.width) / 2;
            }
        }

        MapLocation[] possibleCrumbs = V.rc.senseNearbyCrumbs(-1);
        if(possibleCrumbs.length >= 1) {
            return RobotUtils.closest(possibleCrumbs);
        }

        MapLocation[] possibleSenses = V.rc.senseBroadcastFlagLocations();
        // Arrays.sort(possibleSenses, (MapLocation a, MapLocation b) -> {
        //     return b.distanceSquaredTo(V.rc.getLocation()) - a.distanceSquaredTo(V.rc.getLocation());
        // }); // yes this is supposed to be sorted furthest first
        if(possibleSenses.length > 0) {
            // V.swarmTarget = possibleSenses[(int)Math.sqrt(V.rng.nextInt(possibleSenses.length * possibleSenses.length))];
            MapLocation broadcast = RobotUtils.closest(possibleSenses);
            //MapLocation broadcast = possibleSenses[0];
            V.swarmTarget = new MapLocation(broadcast.x + V.rng.nextInt(11) - 5, broadcast.y + V.rng.nextInt(11) - 5);
            // V.swarmTarget = possibleSenses[0];
            V.swarmEnd = V.rc.getRoundNum() + StrictMath.max(V.height, V.width) / 2;
            return V.swarmTarget;
        }
        return V.centre;
    }
}
