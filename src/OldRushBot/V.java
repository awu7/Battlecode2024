package OldRushBot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToIntBiFunction;

public class V {
    static RobotController rc;
    static Random rng;
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST,
    };
    static Direction[] shuffledDirections = {
            Direction.SOUTHEAST,
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST,
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
    };
    static final String[] dirStrs = {};
    static final Direction[] trapDirs = {
            Direction.SOUTHEAST,
            Direction.NORTHEAST,
            Direction.WEST,
    };

    static MapLocation centre;
    static MapLocation[] spawnCentres = new MapLocation[3];
    static MapLocation home = new MapLocation(-1, -1);
    static boolean isBuilder = false;
    static int movesLeft = 0;
    static MapLocation target;
    static boolean lastFlag = false;
    static int round;
    /**
     * Array of all previous stuns
     */
    static List<MapLocation> prevStuns = new ArrayList<>();
    static List<ActiveStun> activeStuns = new ArrayList<>();
    static RobotInfo[] sittingDucks;

    static String indicatorString = "";

    static int id;
    /**
     * ID compression system.
     * Each robot starts with an ID, theoretically in the range [10000, 14096).
     * ids is an int array which stores the actual ID of each robot.
     * idx is a compressed mapping of each ID to its index in ids
     * ids is sorted by turn order.
     */
    static int[] ids;
    static int[] idx;
    /**
     * [2, 31] = scouters, broadcasts everything seen in allocated spot in array.
     * [45, 49] = BFS bots, receives information from other bots and runs BFS in idle turns.
     */
    static int selfIdx = -1;

    static Direction[] stack = new Direction[10];
    static int stackSize = 0;
    static int turnDir = 0;
    static int stackPassIndex = 3;
    static RobotInfo[] nearbyAllies;

    /**
     * 2d array storing the board
     * <ul>
     * <li>0 = non-wall</li>
     * <li>1 = wall</li>
     * <li>3 = undiscovered</li>
     * </ul>
     */
    static int[][] board;
    /**
     * 2d array storing a simplified version of the board with weights, used for BFS.
     */
    static boolean vertical = true, horizontal = true, rotational = true;
    public enum Symmetry {
        UNKNOWN("Unknown"),
        VERTICAL("Vertical"),
        HORIZONTAL("Horizontal"),
        ROTATIONAL("Rotational");

        public final String label;

        Symmetry(String label) {
            this.label = label;
        }
    }
    static Symmetry symmetry = Symmetry.UNKNOWN;
    static ToIntBiFunction<Integer, Integer> getOpp = (x, y) -> 1;
    static int width, height;
    static int widthMinus1, widthMinus2, widthMinus3;
    static int widthPlus1;
    static int heightMinus1,heightMinus2, heightMinus3;
    static int heightPlus1, heightPlus2;

    static BfsCalc spawnBfs;

    public enum BfsTarget {
        CENTRE,
        SPAWN,
        FLAG0,
        FLAG1,
        FLAG2
    }

    static MapLocation swarmTarget;
    static int swarmEnd = 0;

    static MapLocation targetCell;

    static Team team;
}
