package RushBot;

import battlecode.common.*;

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
        return StrictMath.min(StrictMath.min(loc.x, V.width - 1 - loc.x), StrictMath.min(loc.y, V.height - 1 - loc.y));
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
     * Helper function which finds the closest location to a given location.
     * @param locs an array of locations.
     * @param from location to find the closest location to, from <code>locs</code>
     * @return the closest location to the robot's current location.
     */
    static MapLocation closest(MapLocation[] locs, MapLocation from) {
        if(locs.length == 0) return new MapLocation(-1, -1);
        int mn = 1000000000;
        MapLocation res = locs[0];
        for (int i = 0; i < locs.length; i++) {
            if(!V.rc.onTheMap(locs[i])) continue;
            int dist = locs[i].distanceSquaredTo(from);
            if (dist < mn) {
                mn = dist;
                res = locs[i];
            }
        }
        return res;
    }

    /**
     * Helper function which finds the closest location to the robot's current location.
     * @param locs an array of locations.
     * @return the closest location to the robot's current location.
     */
    static MapLocation closest(MapLocation[] locs) {
        return closest(locs, V.rc.getLocation());
    }

    public static boolean sameTile(int a, int b) {
        return a == 3 || b == 3 || a == b;
    }

    public static boolean maybeOppSpawn(int a) {
        return a == 3 || a == 0;
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
        V.id = V.rc.getID();
        V.team = V.rc.getTeam();
        V.opp = V.team.opponent();
        V.spawns = V.rc.getAllySpawnLocations();
        V.micro = new MicroAttacker();
        V.lastAttackTimestamp = 200;
        BugNav.init();
        Healing.init();
    }

    public static void updateRobots() {
        if (V.rc.isSpawned()) {
            try {
                V.allies = V.rc.senseNearbyRobots(-1, V.team);
                V.enemies = V.rc.senseNearbyRobots(-1, V.opp);
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            }
        } else {
            V.allies = new RobotInfo[]{};
            V.enemies = new RobotInfo[]{};
        }
    }

    public static void startRound() {
        updateRobots();
    }

    public static void endRound() {
        if (V.round > 200) {
            if (!Attacking.attacked && !Healing.healed) {
                if (V.rc.isSpawned()) {
                    if (V.rc.isActionReady() && V.enemies.length > 0) {
                        V.history.append('?');
                    } else {
                        V.history.append(V.rc.getActionCooldownTurns());
                    }
                } else {
                    V.history.append('X');
                }
            }
            V.history.append(',');
        }
        Attacking.attacked = false;
        Healing.healed = false;
        if (V.round == 200) {
            System.out.println(V.rc.getLocation());
        }
        if (V.round == 300) {
            System.out.println(V.id + "," + V.history);
        }
        if (V.rc.onTheMap(V.targetCell) && V.rc.isSpawned()) {
            V.rc.setIndicatorLine(V.rc.getLocation(), V.targetCell, 0, 255, 0);
        }
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

    /**
     * For use by {@link RobotUtils#validFlagPlacement(MapLocation)}
     */
    static MapLocation other1 = null, other2 = null;
    /**
     * Last time {@link RobotUtils#other1} and {@link RobotUtils#other2}
     * were updated
     */
    static int lastUpdateOther = -1;
    /**
     * Senses validFlagPlacements based on information from the comms array.
     * Use this instead of <code>V.rc.senseLegalStartingFlagPlacement()</code>
     * @param loc The MapLocation to check
     * @return <code>true</code> if this is a legal flag starting position, <code>false</code> otherwise
     */
    public static boolean validFlagPlacement(MapLocation loc) throws GameActionException {
        /*if (V.selfIdx < Consts.LOWEST_FLAG_SITTER || V.selfIdx > Consts.HIGHEST_FLAG_SITTER) {
            // This robot isn't even a flag sitter, why call this?
            return false;
        }*/
        if (lastUpdateOther != V.round) {
            RobotUtils.debug("UPDATING CACHE");
            lastUpdateOther = V.round;
            int id = V.selfIdx - 47;
            // switch case is bytecode saver
            switch (id) {
                case 0:
                    other1 = Comms.decode(V.rc.readSharedArray(Consts.LOWEST_FS_COMMS_IDX+1));
                    other2 = Comms.decode(V.rc.readSharedArray(Consts.LOWEST_FS_COMMS_IDX+2));
                    break;
                case 1:
                    other1 = Comms.decode(V.rc.readSharedArray(Consts.LOWEST_FS_COMMS_IDX));
                    other2 = Comms.decode(V.rc.readSharedArray(Consts.LOWEST_FS_COMMS_IDX+2));
                    break;
                case 2:
                    other1 = Comms.decode(V.rc.readSharedArray(Consts.LOWEST_FS_COMMS_IDX));
                    other2 = Comms.decode(V.rc.readSharedArray(Consts.LOWEST_FS_COMMS_IDX+1));
                    break;
            }
        }
        V.rc.setIndicatorDot(other1, 255, 127, 0);
        V.rc.setIndicatorDot(other2, 255, 127, 0);
        return StrictMath.min(loc.distanceSquaredTo(other1), loc.distanceSquaredTo(other2)) > GameConstants.MIN_FLAG_SPACING_SQUARED;
    }
}
