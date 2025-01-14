package GCMicro;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GlobalUpgrade;
import battlecode.common.MapLocation;

import java.util.Comparator;
import java.util.Random;

public class RobotUtils {
    /**
     * Helper function to append to the bot's indicator string.
     * @param x the value to append.
     * @param <T> the type of the value.
     */
    static <T> void debug(T x) {
        V.indicatorString += String.valueOf(x) + "; ";
    }

    /**
     * Helper function to calculate the minimum distance to the edge of
     * the map
     * @param loc Location to find the smallest distance to the edge from
     * @return The smallest distance to the edge of the map
     */
    static int distFromEdge(MapLocation loc) {
        return StrictMath.min(StrictMath.min(loc.x, V.width - loc.x), StrictMath.min(loc.y, V.height - loc.y));
    }

    /**
     * Helper function used in sorting tiles based on distance.
     * @param target the target tile to which distance is calculated
     * @return a Comparator<MapLocation> which can be passed into the sorting function
     */
    static Comparator<MapLocation> closestComp(MapLocation target) {
        return new Comparator<MapLocation>() {
            @Override
            public int compare(MapLocation o1, MapLocation o2) {
                return target.distanceSquaredTo(o1) - target.distanceSquaredTo(o2);
            }
        };
    }

    /**
     * Helper function used in sorting tiles based on distance to robot's current position.
     * @return a Comparator<MapLocation> which can be passed into the sorting function
     */
    static Comparator<MapLocation> closestComp() {
        return closestComp(V.rc.getLocation());
    }

    /**
     * Helper function which finds the closest locatin to the robot's current location.
     * @param locs an array of locations.
     * @return the closest location to the robot's current location.
     */
    static MapLocation closest(MapLocation[] locs) {
        if(locs.length == 0) return new MapLocation(-1, -1);
        int mn = 1000000000;
        MapLocation res = locs[0];
        for (int i = 0; i < locs.length; i++) {
            if(!V.rc.onTheMap(locs[i])) continue;
            int dist = locs[i].distanceSquaredTo(V.rc.getLocation());
            if (dist < mn) {
                mn = dist;
                res = locs[i];
            }
        }
        return res;
    }

    /**
     * Helper function attempting to buy global upgrades.
     * Priority:
     * <ol>
     * <li><code>GlobalUpgrade.ATTACK</code></li>
     * <li><code>GlobalUpgrade.HEALING</code></li>
     * <li><code>GlobalUpgrade.CAPTURING</code></li>
     * </ol>
     * @throws GameActionException
     */
    public static void buyGlobal() throws GameActionException {
        if (V.rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
            V.rc.buyGlobal(GlobalUpgrade.ATTACK);
        }
        if (V.rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
            V.rc.buyGlobal(GlobalUpgrade.HEALING);
        }
        if (V.rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
            V.rc.buyGlobal(GlobalUpgrade.CAPTURING);
        }
    }

    public static boolean sameTile(int a, int b) {
        return a == 3 || b == 3 || a == b;
    }

    public static <T> void shuffle(T[] arr) {
        for (int i = arr.length - 1; i >= 1; --i) {
            int j = V.rng.nextInt(i);
            T temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }

    public static void init() {
        V.rng = new Random(V.rc.getID());
        V.targetCell = new MapLocation(-1, -1);
        V.width = V.rc.getMapWidth();
        V.height = V.rc.getMapHeight();
        V.widthMinus1 = V.width - 1;
        V.widthMinus2 = V.width - 2;
        V.widthMinus3 = V.width - 3;
        V.widthPlus1 = V.width + 1;
        V.heightMinus1 = V.height - 1;
        V.heightMinus2 = V.height - 2;
        V.heightMinus3 = V.height - 3;
        V.heightPlus1 = V.height + 1;
        V.heightPlus2 = V.height + 2;
        V.centre = new MapLocation(V.width / 2, V.height / 2);
        V.board = new int[V.width][];
        for (int x = V.width; --x >= 0;) {
            UnrolledUtils.fill(V.board[x] = new int[V.height], 3);
        }
        for (MapLocation spawn: V.rc.getAllySpawnLocations()) {
            V.board[spawn.x][spawn.y] = 3;
        }
        V.id = V.rc.getID();
    }

    public static void endRound() {
        if (V.rc.onTheMap(V.targetCell) && V.rc.isSpawned()) {
            V.rc.setIndicatorLine(V.rc.getLocation(), V.targetCell, 0, 255, 0);
        }
        RobotUtils.debug("TD: " + String.valueOf(V.turnDir));
        RobotUtils.debug("SS: " + String.valueOf(V.stackSize));
        V.rc.setIndicatorString(V.indicatorString);
        V.indicatorString = "";
        int round = V.rc.getRoundNum();
        if (round != V.round) {
            System.out.println("Overflow! " + V.round + " -> " + round);
        }
    }

    public static boolean isCentreSpawn(MapLocation loc) throws GameActionException {
        for (Direction dir: V.directions) {
            if (!V.rc.canSenseLocation(loc.add(dir))) {
                return false;
            }
            if (!V.rc.senseMapInfo(loc.add(dir)).isSpawnZone()) {
                return false;
            }
        }
        return true;
    }
}
