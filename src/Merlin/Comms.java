package Merlin;

import battlecode.common.*;

/**
 * The communication array consists of 64 slots of 16-bit integers. <br>
 * <b>Please fill in this javadoc with all slots that are being used. </b><br>
 * [3, 5] - Allied flag broadcasting in order to coordinate valid flag placements<br>
 */
public class Comms {
    /**
     * Converts an <code>int</code> into a <code>MapLocation</code>
     * @param encoded The <code>int</code> to decode
     * @return The decoded <code>MapLocation</code>
     */
    public static MapLocation decode(int encoded) {
        return new MapLocation((encoded >>> 6) & 0b111111, encoded & 0b111111);
    }

    /**
     * Converts a <code>MapLocation</code> into an <code>int</code>
     * @param decoded The <code>MapLocation</code> to encode
     * @return The encoded <code>int</code>
     */
    public static int encode(MapLocation decoded) {
        return (decoded.x << 6) | decoded.y;
    }
}
