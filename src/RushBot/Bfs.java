package RushBot;

import battlecode.common.*;

public class Bfs {
    /**
     * Updates the contents of a cell using knowledge of symmetry.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public static void updateCellSymmetry(int x, int y) {
        if (V.symmetry == V.Symmetry.HORIZONTAL) {
            V.board[V.width - 1 - x][y] = V.board[x][y];
        }
        if (V.symmetry == V.Symmetry.VERTICAL) {
            V.board[x][V.height - 1 - y] = V.board[x][y];
        }
        if (V.symmetry == V.Symmetry.ROTATIONAL) {
            V.board[V.width - 1 - x][V.height - 1 - y] = V.board[x][y];
        }
    }

    /**
     * Records the squares in the vision radius seen by the current bot.
     */
    public static void recordVision() {
        for (MapInfo square: V.rc.senseNearbyMapInfos()) {
            int x = square.getMapLocation().x;
            int y = square.getMapLocation().y;
            if (V.board[x][y] == 0) {
                if (square.getSpawnZoneTeamObject() == V.rc.getTeam()) {
                    V.board[x][y] = 3;
                } else if (square.getSpawnZoneTeamObject() == V.rc.getTeam().opponent()) {
                    V.board[x][y] = 4;
                } else if (square.isWall()) {
                    V.board[x][y] = 2;
                } else if (!square.isWater() && !square.isPassable()) {
                    // dam
                    V.board[x][y] = 5;
                } else {
                    V.board[x][y] = 1;
                }

                if (V.symmetry == V.Symmetry.UNKNOWN) {
                    int ver = V.board[x][V.height - 1 - y];
                    if (!RobotUtils.sameTile(V.board[x][y], ver)) {
                        V.vertical = false;
                        if (!V.horizontal) {
                            V.symmetry = V.Symmetry.ROTATIONAL;
                        } else if (!V.rotational) {
                            V.symmetry = V.Symmetry.HORIZONTAL;
                        }
                    }
                    int hor = V.board[V.width - 1 - x][y];
                    if (!RobotUtils.sameTile(V.board[x][y], hor)) {
                        V.horizontal = false;
                        if (!V.vertical) {
                            V.symmetry = V.Symmetry.ROTATIONAL;
                        } else if (!V.rotational) {
                            V.symmetry = V.Symmetry.VERTICAL;
                        }
                    }
                    int rot = V.board[V.width - 1 - x][V.height - 1 - y];
                    if (!RobotUtils.sameTile(V.board[x][y], rot)) {
                        V.rotational = false;
                        if (!V.horizontal) {
                            V.symmetry = V.Symmetry.VERTICAL;
                        } else if (!V.vertical) {
                            V.symmetry = V.Symmetry.HORIZONTAL;
                        }
                    }
                } else {
                    updateCellSymmetry(x, y);
                }
            }
        }
    }

    public static void broadcastVision(int arrayIdx) throws GameActionException {
        int x = V.rc.getLocation().x, y = V.rc.getLocation().y;
        int hash1 = x * V.height + y, hash2 = 0;
        V.rc.writeSharedArray(arrayIdx, hash1);
        MapLocation loc;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-2, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-1, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 1;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(0, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 2;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(1, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 3;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(2, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 4;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, 2)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 5;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, 1)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 6;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, 0)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 7;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, -1)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 8;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(3, -2)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 9;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(2, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 10;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(1, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 11;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(0, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 12;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-1, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 13;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-2, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 14;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, -2)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 15;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, -1)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 12;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, 0)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 13;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, 1)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 14;
        if(V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, 2)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 15;
        V.rc.writeSharedArray(arrayIdx, hash1);
        V.rc.writeSharedArray(arrayIdx | 1, hash2);
    }

    public static void decodeBroadcast(int arrIdx) throws GameActionException {
        int hash1 = V.rc.readSharedArray(arrIdx), hash2 = V.rc.readSharedArray(arrIdx | 1);
        int locHash = hash1 & ((1 << 12) - 1);
        int x = locHash / V.height, y = locHash % V.height;
        if (V.symmetry == V.Symmetry.VERTICAL) {
            if (x >= 2 && y < V.heightMinus3) V.board[x - 2][V.heightMinus1 - (y + 3)] = V.board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < V.heightMinus3) V.board[x - 1][V.heightMinus1 - (y + 3)] = V.board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < V.heightMinus3) V.board[x][V.heightMinus1 - (y + 3)] = V.board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < V.widthMinus1 && y < V.heightMinus3) V.board[x + 1][V.heightMinus1 - (y + 3)] = V.board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < V.widthMinus2 && y < V.heightMinus3) V.board[x + 2][V.heightMinus1 - (y + 3)] = V.board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus2) V.board[x + 3][V.heightMinus1 - (y + 2)] = V.board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus1) V.board[x + 3][V.heightMinus1 - (y + 1)] = V.board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < V.widthMinus3) V.board[x + 3][V.heightMinus1 - (y)] = V.board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < V.widthMinus3 && y >= 1) V.board[x + 3][V.heightMinus1 - (y - 1)] = V.board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < V.widthMinus3 && y >= 2) V.board[x + 3][V.heightMinus1 - (y - 2)] = V.board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < V.widthMinus2 && y >= 3) V.board[x + 2][V.heightMinus1 - (y - 3)] = V.board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < V.widthMinus1 && y >= 3) V.board[x + 1][V.heightMinus1 - (y - 3)] = V.board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) V.board[x][V.heightMinus1 - (y - 3)] = V.board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) V.board[x - 1][V.heightMinus1 - (y - 3)] = V.board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) V.board[x - 2][V.heightMinus1 - (y - 3)] = V.board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) V.board[x - 3][V.heightMinus1 - (y - 2)] = V.board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) V.board[x - 3][V.heightMinus1 - (y - 1)] = V.board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) V.board[x - 3][V.heightMinus1 - (y)] = V.board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < V.heightMinus1) V.board[x - 3][V.heightMinus1 - (y + 1)] = V.board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < V.heightMinus2) V.board[x - 3][V.heightMinus1 - (y + 2)] = V.board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        } else if (V.symmetry == V.Symmetry.HORIZONTAL) {
            if (x >= 2 && y < V.heightMinus3) V.board[V.widthMinus1 - (x - 2)][y + 3] = V.board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < V.heightMinus3) V.board[V.widthMinus1 - (x - 1)][y + 3] = V.board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < V.heightMinus3) V.board[V.widthMinus1 - (x)][y + 3] = V.board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < V.widthMinus1 && y < V.heightMinus3) V.board[V.widthMinus1 - (x + 1)][y + 3] = V.board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < V.widthMinus2 && y < V.heightMinus3) V.board[V.widthMinus1 - (x + 2)][y + 3] = V.board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus2) V.board[V.widthMinus1 - (x + 3)][y + 2] = V.board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus1) V.board[V.widthMinus1 - (x + 3)][y + 1] = V.board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < V.widthMinus3) V.board[V.widthMinus1 - (x + 3)][y] = V.board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < V.widthMinus3 && y >= 1) V.board[V.widthMinus1 - (x + 3)][y - 1] = V.board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < V.widthMinus3 && y >= 2) V.board[V.widthMinus1 - (x + 3)][y - 2] = V.board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < V.widthMinus2 && y >= 3) V.board[V.widthMinus1 - (x + 2)][y - 3] = V.board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < V.widthMinus1 && y >= 3) V.board[V.widthMinus1 - (x + 1)][y - 3] = V.board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) V.board[V.widthMinus1 - (x)][y - 3] = V.board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) V.board[V.widthMinus1 - (x - 1)][y - 3] = V.board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) V.board[V.widthMinus1 - (x - 2)][y - 3] = V.board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) V.board[V.widthMinus1 - (x - 3)][y - 2] = V.board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) V.board[V.widthMinus1 - (x - 3)][y - 1] = V.board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) V.board[V.widthMinus1 - (x - 3)][y] = V.board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < V.heightMinus1) V.board[V.widthMinus1 - (x - 3)][y + 1] = V.board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < V.heightMinus2) V.board[V.widthMinus1 - (x - 3)][y + 2] = V.board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        } else if (V.symmetry == V.Symmetry.ROTATIONAL) {
            if (x >= 2 && y < V.heightMinus3) V.board[V.widthMinus1 - (x - 2)][V.heightMinus1 - (y + 3)] = V.board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < V.heightMinus3) V.board[V.widthMinus1 - (x - 1)][V.heightMinus1 - (y + 3)] = V.board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < V.heightMinus3) V.board[V.widthMinus1 - (x)][V.heightMinus1 - (y + 3)] = V.board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < V.widthMinus1 && y < V.heightMinus3) V.board[V.widthMinus1 - (x + 1)][V.heightMinus1 - (y + 3)] = V.board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < V.widthMinus2 && y < V.heightMinus3) V.board[V.widthMinus1 - (x + 2)][V.heightMinus1 - (y + 3)] = V.board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus2) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y + 2)] = V.board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus1) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y + 1)] = V.board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < V.widthMinus3) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y)] = V.board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < V.widthMinus3 && y >= 1) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y - 1)] = V.board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < V.widthMinus3 && y >= 2) V.board[V.widthMinus1 - (x + 3)][V.heightMinus1 - (y - 2)] = V.board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < V.widthMinus2 && y >= 3) V.board[V.widthMinus1 - (x + 2)][V.heightMinus1 - (y - 3)] = V.board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < V.widthMinus1 && y >= 3) V.board[V.widthMinus1 - (x + 1)][V.heightMinus1 - (y - 3)] = V.board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) V.board[V.widthMinus1 - (x)][V.heightMinus1 - (y - 3)] = V.board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) V.board[V.widthMinus1 - (x - 1)][V.heightMinus1 - (y - 3)] = V.board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) V.board[V.widthMinus1 - (x - 2)][V.heightMinus1 - (y - 3)] = V.board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y - 2)] = V.board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y - 1)] = V.board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y)] = V.board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < V.heightMinus1) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y + 1)] = V.board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < V.heightMinus2) V.board[V.widthMinus1 - (x - 3)][V.heightMinus1 - (y + 2)] = V.board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        } else {
            if (x >= 2 && y < V.heightMinus3) V.board[x - 2][y + 3] = hash2 & 1;
            if (x >= 1 && y < V.heightMinus3) V.board[x - 1][y + 3] = (hash2 >>> 1) & 1;
            if (x >= 0 && y < V.heightMinus3) V.board[x][y + 3] = (hash2 >>> 2) & 1;
            if (x < V.widthMinus1 && y < V.heightMinus3) V.board[x + 1][y + 3] = (hash2 >>> 3) & 1;
            if (x < V.widthMinus2 && y < V.heightMinus3) V.board[x + 2][y + 3] = (hash2 >>> 4) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus2) V.board[x + 3][y + 2] = (hash2 >>> 5) & 1;
            if (x < V.widthMinus3 && y < V.heightMinus1) V.board[x + 3][y + 1] = (hash2 >>> 6) & 1;
            if (x < V.widthMinus3) V.board[x + 3][y] = (hash2 >>> 7) & 1;
            if (x < V.widthMinus3 && y >= 1) V.board[x + 3][y - 1] = (hash2 >>> 8) & 1;
            if (x < V.widthMinus3 && y >= 2) V.board[x + 3][y - 2] = (hash2 >>> 9) & 1;
            if (x < V.widthMinus2 && y >= 3) V.board[x + 2][y - 3] = (hash2 >>> 10) & 1;
            if (x < V.widthMinus1 && y >= 3) V.board[x + 1][y - 3] = (hash2 >>> 11) & 1;
            if (x >= 0 && y >= 3) V.board[x][y - 3] = (hash2 >>> 12) & 1;
            if (x >= 1 && y >= 3) V.board[x - 1][y - 3] = (hash2 >>> 13) & 1;
            if (x >= 2 && y >= 3) V.board[x - 2][y - 3] = (hash2 >>> 14) & 1;
            if (x >= 3 && y >= 2) V.board[x - 3][y - 2] = (hash2 >>> 15) & 1;
            if (x >= 3 && y >= 1) V.board[x - 3][y - 1] = (hash1 >>> 12) & 1;
            if (x >= 3) V.board[x - 3][y] = (hash1 >>> 13) & 1;
            if (x >= 3 && y < V.heightMinus1) V.board[x - 3][y + 1] = (hash1 >>> 14) & 1;
            if (x >= 3 && y < V.heightMinus2) V.board[x - 3][y + 2] = (hash1 >>> 15) & 1;
        }
    }

    /**
     * Helper function to broadcast the current discovered symmetry to the BFS bots.
     * @param arrIdx the index at which the symmetry is written in the global array.
     * @throws GameActionException
     */
    public static void broadcastSymmetry(int arrIdx) throws GameActionException {
        int hash = 0;
        if (V.vertical) hash |= 1;
        if (V.horizontal) hash |= 1 << 1;
        if (V.rotational) hash |= 1 << 2;
    }

    public static void decodeSymmetry(int arrIdx) throws GameActionException {
        int hash = V.rc.readSharedArray(arrIdx);
        if ((hash & 1) == 0) V.vertical = false;
        if (((hash >>> 1) & 1) == 0) V.horizontal = false;
        if (((hash >>> 2) & 1) == 0) V.rotational = false;
    }

    public static void broadcastBfs() throws GameActionException {
        for (int i = 4; i < 64; ++i) {
            int hash = 0;
            hash |= V.optimal[V.internalIdx / V.height][V.internalIdx % V.height];
            if (++V.internalIdx == V.width * V.height) break;
            hash |= V.optimal[V.internalIdx / V.height][V.internalIdx % V.height] << 4;
            if (++V.internalIdx == V.width * V.height) break;
            hash |= V.optimal[V.internalIdx / V.height][V.internalIdx % V.height] << 8;
            if (++V.internalIdx == V.width * V.height) break;
            hash |= V.optimal[V.internalIdx / V.height][V.internalIdx % V.height] << 12;
            if (++V.internalIdx == V.width * V.height) break;
            V.rc.writeSharedArray(i, hash);
        }
        if (V.internalIdx == V.width * V.height) {
            switch (V.selfIdx) {
                case 45:
                    V.centreBfs = V.optimal;
                    break;
                case 46:
                    V.spawnBfs = V.optimal;
                    break;
                case 47:
                    V.flag0Bfs = V.optimal;
                    break;
                case 48:
                    V.flag1Bfs = V.optimal;
                    break;
                case 49:
                    V.flag2Bfs = V.optimal;
                    break;
            }
            V.bfsIdx = -3;
            V.internalIdx = 0;
        }
    }

    public static void receiveBfs() throws GameActionException {
        if (V.internalIdx == 0) {
            switch (V.bfsIdx) {
                case 45:
                    V.centreBfs = new int[V.width][V.height];
                    break;
                case 46:
                    V.spawnBfs = new int[V.width][V.height];
                    break;
                case 47:
                    V.flag0Bfs = new int[V.width][V.height];
                    break;
                case 48:
                    V.flag1Bfs = new int[V.width][V.height];
                    break;
                case 49:
                    V.flag2Bfs = new int[V.width][V.height];
                    break;
                default:
                    System.out.println("Something went wrong!");
            }
        }
        int[][] thisBfs;
        switch (V.bfsIdx) {
            case 45:
                thisBfs = V.centreBfs;
                break;
            case 46:
                thisBfs = V.spawnBfs;
                break;
            case 47:
                thisBfs = V.flag0Bfs;
                break;
            case 48:
                thisBfs = V.flag1Bfs;
                break;
            case 49:
                thisBfs = V.flag2Bfs;
                break;
            default:
                thisBfs = new int[V.width][V.height];
                System.out.println("Something went wrong!");
        }
        int mask = (1 << 4) - 1;
        for (int i = 4; i < 64; ++i) {
            int hash = V.rc.readSharedArray(i);
            thisBfs[V.internalIdx / V.height][V.internalIdx % V.height] = hash & mask;
            if (++V.internalIdx == V.width * V.height) break;
            thisBfs[V.internalIdx / V.height][V.internalIdx % V.height] = (hash >>> 4) & mask;
            if (++V.internalIdx == V.width * V.height) break;
            thisBfs[V.internalIdx / V.height][V.internalIdx % V.height] = (hash >>> 8) & mask;
            if (++V.internalIdx == V.width * V.height) break;
            thisBfs[V.internalIdx / V.height][V.internalIdx % V.height] = (hash >>> 12) & mask;
            if (++V.internalIdx == V.width * V.height) break;
        }
        if (V.internalIdx == V.width * V.height) {
            V.bfsIdx++;
            V.internalIdx = 0;
        }
    }

    public static void moveBfsUtil(int[][] bfsArr, V.BfsTarget target) throws GameActionException {
        //printBoard();
        if (!V.rc.isMovementReady()) {
            return;
        }
        MapLocation loc = V.rc.getLocation();
        RobotInfo[] friends = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
        int i = bfsArr != null ? bfsArr[loc.x][loc.y] : 0;
        if (i > 0) {
            RobotUtils.debug("Moving using bfs");
            System.out.println("Moving using bfs");
            Direction dir = V.directions[i - 1];
            V.rc.setIndicatorLine(V.rc.getLocation(), loc.add(dir), 0, 0, 255);
            Direction[] choices = i - 1 < 4 ? new Direction[]{dir, dir.rotateLeft(), dir.rotateRight()} : new Direction[]{dir};
            for (Direction choice: choices) {
                if (V.rc.canMove(choice)) {
                    V.rc.move(choice);
                    System.out.println("Moved to " + V.rc.getLocation().x + ", " + V.rc.getLocation().y + ": " + i);
                    return;
                }
            }
            for (Direction choice: choices) {
                MapLocation next = loc.add(choice);
                if(!V.rc.onTheMap(next)) continue;
                int j = bfsArr[next.x][next.y];
                if (j > 0) {
                    Direction nextDir = V.directions[j - 1];
                    for (Direction nextChoice: new Direction[]{nextDir, nextDir.rotateLeft(), nextDir.rotateRight()}) {
                        MapLocation nextNext = next.add(nextChoice);
                        if (V.rc.canDropFlag(nextNext)) {
                            RobotInfo friend = V.rc.senseRobotAtLocation(nextNext);
                            if (friend != null && friend.getTeam() == V.rc.getTeam()) {
                                V.rc.dropFlag(next);
                                RobotUtils.debug("Passed the flag to a spot");
                                return;
                            }
                            System.out.println("No friend at " + nextNext.x + ", " + nextNext.y);
                        } else System.out.println("Can't drop flag at " + nextNext.x + ", " + nextNext.y);
                    }
                }
                if (V.rc.canDropFlag(next)) {
                    RobotInfo friend = V.rc.senseRobotAtLocation(next);
                    if (friend != null && friend.getTeam() == V.rc.getTeam()) {
                        V.rc.dropFlag(next);
                        RobotUtils.debug("Passed the flag in bfs");
                        return;
                    }
                    System.out.println("No friend at " + next.x + ", " + next.y);
                } else System.out.println("Can't drop flag at " + next.x + ", " + next.y);
            }
        }
        System.out.println("Can't bfs, defaulting");
        switch (target) {
            case CENTRE:
                BugNav.moveBetter(V.centre);
                break;
            case SPAWN:
                BugNav.moveBetter(RobotUtils.closest(V.rc.getAllySpawnLocations()));
                break;
            case FLAG0:
            case FLAG1:
            case FLAG2:
                BugNav.moveBetter(new MapLocation(1, 1));
                break;
            default:
                System.out.println("Something went wrong in moveBfsUtil()!");
        }
    }

    public static void moveBfs(V.BfsTarget target) throws GameActionException {
        switch (target) {
            case CENTRE:
                moveBfsUtil(V.centreBfs, target);
                break;
            case SPAWN:
                moveBfsUtil(V.spawnBfs, target);
                break;
            case FLAG0:
                moveBfsUtil(V.flag0Bfs, target);
                break;
            case FLAG1:
                moveBfsUtil(V.flag1Bfs, target);
                break;
            case FLAG2:
                moveBfsUtil(V.flag2Bfs, target);
                break;
            default:
                System.out.println("Something went wrong in moveBfs()!");
        }
    }

    public static MapLocation unhashChar(char c) {
        return new MapLocation(c / V.height, c % V.height);
    }

    public static char hashLoc(MapLocation loc) {
        return (char) (loc.x * V.height + loc.y);
    }

    public static boolean precomp() throws GameActionException {
        if (V.round >= 3 && V.round <= 160) {
            if (V.rc.isSpawned()) {
                if (V.selfIdx >= 45) {
                    for (int i = 4; i < 64; i += 2) {
                        Bfs.decodeBroadcast(i);
                    }
                } else {
                    Bfs.recordVision();
                    if (V.selfIdx >= 2 && V.selfIdx <= 31) {
                        Bfs.broadcastVision(V.selfIdx << 1);
                    }
                }
            }
        } else if (V.round == 161) {
            if (V.selfIdx <= 44) {
                // all robots broadcast their interpretation of the map's symmetry
                Bfs.broadcastSymmetry(V.selfIdx + 19);
            } else {
                // receive symmetry broadcasts from other robots
                for (int i = 19; i < 64; ++i) {
                    Bfs.decodeSymmetry(i);
                }
                if (V.symmetry == V.Symmetry.VERTICAL) {
                    V.getOpp = (x, y) -> V.board[x][V.heightMinus1 - y];
                } else if (V.symmetry == V.Symmetry.HORIZONTAL) {
                    V.getOpp = (x, y) -> V.board[V.widthMinus1 - x][y];
                } else {
                    V.getOpp = (x, y) -> V.board[V.widthMinus1 - x][V.heightMinus1 - y];
                }
            }
            if (V.selfIdx >= 45) {
                V.q = new StringBuilder();
                MapLocation loc = new MapLocation(1, 1);  // will use Aiden's centre spawns once pulled
                V.bfs = new int[V.width][V.height];
                for (int x = 0; x < V.width; ++x) {
                    UnrolledUtils.fill(V.bfs[x], -1);
                }
                if (V.selfIdx == 45) {
                    V.q.append(hashLoc(V.centre));
                    V.bfs[V.centre.x][V.centre.y] = 0;
                } else if (V.selfIdx == 46) {
                    for (MapLocation spawn: V.rc.getAllySpawnLocations()) {
                        V.q.append(hashLoc(spawn));
                        V.bfs[spawn.x][spawn.y] = 0;
                    }
                } else if (V.selfIdx == 47) {
                    V.q.append(hashLoc(loc));
                } else if (V.selfIdx == 48) {
                    V.q.append(hashLoc(loc));
                } else if (V.selfIdx == 49) {
                    V.q.append(hashLoc(loc));
                }
                V.optimal = new int[V.width][V.height];
                V.rc.writeSharedArray(V.selfIdx + 14, 0);
                return false;
            }
        } else if (V.round >= 162 && V.bfsIdx < 50) {
            if (V.selfIdx >= 45 && !V.bfsDone) {
                // BFS time baby!
                while (V.q.length() > 0) {
                    int x1 = V.q.charAt(0) / V.height, y1 = V.q.charAt(0) % V.height;
                    V.q.deleteCharAt(0);
                    int nextDist = V.bfs[x1][y1] + 1;
                    for (int i = 0; i < 8; ++i) {
                        int x2 = x1 + V.directions[i].opposite().getDeltaX(), y2 = y1 + V.directions[i].opposite().getDeltaY();
                        if (x2 >= 0 && x2 < V.width && y2 >= 0 && y2 < V.height) {
                            if ((V.board[x2][y2] | V.getOpp.applyAsInt(x2, y2)) == 0 && V.bfs[x2][y2] == -1) {
                                V.bfs[x2][y2] = nextDist;
                                V.optimal[x2][y2] = i + 1;
                                V.q.append((char) (x2 * V.height + y2));
                            }
                        }
                    }
                    if (Clock.getBytecodesLeft() < 1000) {
                        break;
                    }
                }
                if (V.q.length() > 0) {
                    return false;
                } else {
                    V.bfsDone = true;
                    V.rc.writeSharedArray(V.selfIdx + 14, 1);
                    V.bfsIdx = -1;
                }
                return false;
            }
            if (V.bfsIdx == -2) {
                V.bfsIdx = -1;
            } else if (V.bfsIdx == -1) {
                V.bfsIdx = 45;
                for (int i = 59; i < 64; ++i) {
                    if (V.rc.readSharedArray(i) == 0) {
                        V.bfsIdx = -1;
                        break;
                    }
                }
                if (V.bfsIdx != -1) {
                    System.out.println(V.selfIdx + ", ready!");
                    if (V.bfsIdx == V.selfIdx) {
                        Bfs.broadcastBfs();
                    } else if (V.bfsIdx < V.selfIdx) {
                        Bfs.receiveBfs();
                    }
                }
            } else if (V.bfsIdx == V.selfIdx) {
                Bfs.broadcastBfs();
            } else if (V.bfsIdx == -3) {
                V.bfsIdx = V.selfIdx + 1;
            } else {
                Bfs.receiveBfs();
            }
        }
        return true;
    }
}
