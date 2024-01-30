package Merlin;

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
    /**
     * The home (centre of a spawnpoint) of the current robot.
     * This is (-1, -1) if the robot does not sit on a flag.
     * Is set once and never again on round 2.
     */
    static MapLocation home = null;
    /**
     * The respawn location of the current flag that the robot
     * is guarding ((-1, -1) if the robot does not sit on a flag).
     * Difference between this and {@link V#home} is that <code>home</code>
     * is only set once, while this changes depending on the location of
     * the flag's respawn.
     */
    static MapLocation flagHome = null;
    /**
     * If this is a flag sitting robot, whether it has
     * decided on a good place to position the flag
     */
    static boolean setFlag = false;
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
     * ID compression system.<br>
     * Each robot starts with an ID, theoretically in the range [10000, 14096).<br>
     * <code>ids</code> is an int array which stores the actual ID of each robot.<br>
     * <code>idx</code> is a compressed mapping of each ID to its index in ids<br>
     * <code>ids</code> is sorted by turn order.
     */
    static int[] ids;
    static int[] idx;
    /**
     * [2, 31] = scouters, broadcasts everything seen in allocated spot in array.<br>
     * [47, 49] = flag sitters, move the flags in setup and sit on them for the rest of the game.
     */
    static int selfIdx = -1;

    static RobotInfo[] nearbyAllies;

    /**
     * 2d array storing the board
     * <ul>
     * <li>0 = non-wall</li>
     * <li>1 = wall</li>
     * <li>3 = undiscovered</li>
     * </ul>
     * 2d array storing a simplified version of the board with weights, used for BFS.
     */
    static int[][] board;

    /**
     * Used as a cache for weightings of walls when choosing flag placements
     */
    static int[][] wallWeights;
    /**
     * Used as a cache for the suitability of a tile as a flag spawn point
     */
    static int[][] flagWeights;
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

    static BfsCalc spawnBfs = null;
    static BfsCalc flagBfs = null;

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

    static Team team, opp;

    /**
     * The timestamp (in rounds) of the last attack performed by this duck.
     */
    static int lastAttackTimestamp;

    static MapLocation[] spawns;

    static Micro micro;

    static RobotInfo[] allies, enemies;

    public static StringBuilder history = new StringBuilder();

    public static final int[] hurt = {600, 600, 600, 600, 450, 450, 450};
    public static final int[] severelyHurt = {300, 300, 300, 300, 450, 450, 450};

    public static MapInfo[] mapInfos;
}
