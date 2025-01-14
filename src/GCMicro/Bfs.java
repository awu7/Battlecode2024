package GCMicro;

import battlecode.common.*;

public class Bfs {
    /**
     * Records the squares in the vision radius seen by the current bot.
     */
    public static void recordVision() {
        for (MapInfo square: V.rc.senseNearbyMapInfos()) {
            int x = square.getMapLocation().x;
            int y = square.getMapLocation().y;
            if (V.board[x][y] == 3) {
                V.board[x][y] = square.isWall() ? 1 : 0;

                V.vertical = V.vertical && RobotUtils.sameTile(V.board[x][y], V.board[x][V.heightMinus1 - y]);
                V.horizontal = V.horizontal && RobotUtils.sameTile(V.board[x][y], V.board[V.widthMinus1 - x][y]);
                V.rotational = V.rotational && RobotUtils.sameTile(V.board[x][y], V.board[V.widthMinus1 - x][V.heightMinus1 - y]);
            }
        }
    }

    public static void broadcastVision(int arrayIdx) throws GameActionException {
        if (arrayIdx < 4 || arrayIdx >= 64) {
            return;
        }
        MapLocation curLoc = V.rc.getLocation();
        int hash1 = curLoc.x << 6 | curLoc.y, hash2 = 0;
        V.rc.writeSharedArray(arrayIdx, hash1);
        MapLocation loc;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(-2, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(-1, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 1;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(0, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 2;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(1, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 3;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(2, 3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 4;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(3, 2)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 5;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(3, 1)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 6;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(3, 0)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 7;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(3, -1)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 8;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(3, -2)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 9;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(2, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 10;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(1, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 11;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(0, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 12;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(-1, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 13;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(-2, -3)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 14;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, -2)) && V.rc.senseMapInfo(loc).isWall()) hash2 |= 1 << 15;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, -1)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 12;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, 0)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 13;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, 1)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 14;
        if (V.rc.onTheMap(loc = V.rc.getLocation().translate(-3, 2)) && V.rc.senseMapInfo(loc).isWall()) hash1 |= 1 << 15;
        V.rc.writeSharedArray(arrayIdx, hash1);
        V.rc.writeSharedArray(arrayIdx | 1, hash2);
    }

    public static void decodeBroadcast(int arrIdx) throws GameActionException {
        int hash1 = V.rc.readSharedArray(arrIdx), hash2 = V.rc.readSharedArray(arrIdx | 1);
        int x = (Integer.rotateRight(hash1, 6)) & 0b111111, y = hash1 & 0b111111;
        int ym3 = y - 3, ym2 = y - 2, ym1 = y - 1, yp1 = y + 1, yp2 = y + 2, yp3 = y + 3;
        int[][] local = V.board;
        int wm1 = V.widthMinus1;
        int wm2 = V.widthMinus2;
        int h = V.height;
        int[] rowx = local[x];
        int[] rowm2 = null, rowm1 = null, rowp1 = null, rowp2 = null;
        if (yp3 < h) {
            if (x >= 2) {
                (rowm2 = local[x - 2])[yp3] = hash2 & 1;
                (rowm1 = local[x - 1])[yp3] = Integer.rotateRight(hash2, 1) & 1;
            } else if (x - 1 == 0) {
                (rowm1 = local[0])[yp3] = Integer.rotateRight(hash2, 1) & 1;
            }
            rowx[yp3] = Integer.rotateRight(hash2, 2) & 1;
            if (x < wm2) {
                (rowp1 = local[x + 1])[yp3] = Integer.rotateRight(hash2, 3) & 1;
                (rowp2 = local[x + 2])[yp3] = Integer.rotateRight(hash2, 4) & 1;
            } else if (x == wm2) {
                (rowp1 = local[wm1])[yp3] = Integer.rotateRight(hash2, 3) & 1;
            }
        }
        if (x + 3 < V.width) {
            int[] row = local[x + 3];
            if (yp2 < h) {
                row[yp2] = Integer.rotateRight(hash2, 5) & 1;
                row[yp1] = Integer.rotateRight(hash2, 6) & 1;
            } else if (yp2 == h) {
                row[yp1] = Integer.rotateRight(hash2, 6) & 1;
            }
            row[y] = Integer.rotateRight(hash2, 7) & 1;
            if (ym2 >= 0) {
                row[ym1] = Integer.rotateRight(hash2, 8) & 1;
                row[ym2] = Integer.rotateRight(hash2, 9) & 1;
            } else if (ym1 == 0) {
                row[0] = Integer.rotateRight(hash2, 8) & 1;
            }
        }
        if (ym3 >= 0) {
            if (rowp2 != null) {
                rowp2[ym3] = Integer.rotateRight(hash2, 10) & 1;
                rowp1[ym3] = Integer.rotateRight(hash2, 11) & 1;
            } else if (rowp1 != null) {
                rowp1[ym3] = Integer.rotateRight(hash2, 11) & 1;
            }
            rowx[ym3] = Integer.rotateRight(hash2, 12) & 1;
            if (rowm2 != null) {
                rowm1[ym3] = Integer.rotateRight(hash2, 13) & 1;
                rowm2[ym3] = Integer.rotateRight(hash2, 14) & 1;
            } else if (rowm1 != null) {
                rowm1[ym3] = Integer.rotateRight(hash2, 13) & 1;
            }
        }
        if (x - 3 >= 0) {
            int[] row = local[x - 3];
            if (ym2 >= 0) {
                row[ym2] = Integer.rotateRight(hash2, 15) & 1;
                row[ym1] = Integer.rotateRight(hash1, 12) & 1;
            } else if (ym1 == 0) {
                row[0] = Integer.rotateRight(hash1, 12) & 1;
            }
            row[y] = Integer.rotateRight(hash1, 13) & 1;
            if (yp2 < h) {
                row[yp1] = Integer.rotateRight(hash1, 14) & 1;
                row[yp2] = Integer.rotateRight(hash1, 15) & 1;
            } else if (yp2 == h) {
                row[yp1] = Integer.rotateRight(hash1, 14) & 1;
            }
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
        V.rc.writeSharedArray(arrIdx, hash);
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
//        switch (target) {
//            case CENTRE:
//                moveBfsUtil(V.centreBfs, target);
//                break;
//            case SPAWN:
//                moveBfsUtil(V.spawnBfs, target);
//                break;
//            case FLAG0:
//                moveBfsUtil(V.flag0Bfs, target);
//                break;
//            case FLAG1:
//                moveBfsUtil(V.flag1Bfs, target);
//                break;
//            case FLAG2:
//                moveBfsUtil(V.flag2Bfs, target);
//                break;
//            default:
//                System.out.println("Something went wrong in moveBfs()!");
//        }
    }

    public static boolean precomp() throws GameActionException {
        if (V.round == 3) {
            int i = V.selfIdx << 1;
            if (V.rc.isSpawned()) {
                Bfs.recordVision();
                Bfs.broadcastVision(i);
            }
            if (i > 64) {
                i = 64;
            }
            for (int j = 4; j < i; j += 2) {
                Bfs.decodeBroadcast(j);
            }
        } else if (V.round >= 4 && V.round < Consts.BFS_ROUND) {
            int i = V.selfIdx << 1;
            if (V.rc.isSpawned()) {
                Bfs.recordVision();
                Bfs.broadcastVision(i);
            }
            if (i < 2) {
                i = 2;
            } else if (i > 64) {
                i = 64;
            }
            for (int j = 4; j < i; j += 2) {
                Bfs.decodeBroadcast(j);
            }
            for (int j = i + 2; j < 64; j += 2) {
                Bfs.decodeBroadcast(j);
            }
        } else if (V.round == Consts.BFS_ROUND) {
            // all robots broadcast their interpretation of the map's symmetry
            Bfs.broadcastSymmetry(V.selfIdx + 14);
        } else if (V.round == Consts.BFS_ROUND + 1) {
            // receive symmetry broadcasts from other robots
            int hash = 0b111;
            for (int i = 14; i < 64; ++i) {
                int hashi = V.rc.readSharedArray(i);
                if (hashi <= 0b111) {
                    hash &= hashi;
                }
            }
            V.vertical = (hash & 1) == 1;
            V.horizontal = (Integer.rotateRight(hash, 1) & 1) == 1;
            V.rotational = (Integer.rotateRight(hash, 2) & 1) == 1;
            if (V.vertical && !V.horizontal && !V.rotational) {
                V.symmetry = V.Symmetry.VERTICAL;
                V.getOpp = (x, y) -> V.board[x][V.heightMinus1 - y];
            } else if (!V.vertical && V.horizontal && !V.rotational) {
                V.symmetry = V.Symmetry.HORIZONTAL;
                V.getOpp = (x, y) -> V.board[V.widthMinus1 - x][y];
            } else if (!V.vertical && !V.horizontal && V.rotational) {
                V.symmetry = V.Symmetry.ROTATIONAL;
                V.getOpp = (x, y) -> V.board[V.widthMinus1 - x][V.heightMinus1 - y];
            }
        } else if (V.round == Consts.BFS_ROUND + 2) {
            switch (V.symmetry) {
                case VERTICAL:
                case HORIZONTAL:
                case ROTATIONAL:
                    updateRotSymmetry();
                    break;
            }
            return false;
        } else if (V.round == Consts.BFS_ROUND + 3) {
            V.spawnBfs = new BfsCalc(V.board, V.rc.getAllySpawnLocations());
        } else if (V.round > Consts.BFS_ROUND + 3 && !V.spawnBfs.done) {
            if (V.rc.isSpawned()) {
                V.spawnBfs.compute(10000);
            } else {
                V.spawnBfs.compute(500);
                return false;
            }
        }
        return true;
    }

    public static void updateRotSymmetry() {
        int[][] local = V.board;
        for (int x = V.widthPlus1 / 2; --x >= 0;) {
            int[] row = local[x];
            int[] orow = local[V.widthMinus1 - x];
            switch (V.height) {
                case 60:
                    orow[0] = row[59] &= orow[0];
                    orow[1] = row[58] &= orow[1];
                    orow[2] = row[57] &= orow[2];
                    orow[3] = row[56] &= orow[3];
                    orow[4] = row[55] &= orow[4];
                    orow[5] = row[54] &= orow[5];
                    orow[6] = row[53] &= orow[6];
                    orow[7] = row[52] &= orow[7];
                    orow[8] = row[51] &= orow[8];
                    orow[9] = row[50] &= orow[9];
                    orow[10] = row[49] &= orow[10];
                    orow[11] = row[48] &= orow[11];
                    orow[12] = row[47] &= orow[12];
                    orow[13] = row[46] &= orow[13];
                    orow[14] = row[45] &= orow[14];
                    orow[15] = row[44] &= orow[15];
                    orow[16] = row[43] &= orow[16];
                    orow[17] = row[42] &= orow[17];
                    orow[18] = row[41] &= orow[18];
                    orow[19] = row[40] &= orow[19];
                    orow[20] = row[39] &= orow[20];
                    orow[21] = row[38] &= orow[21];
                    orow[22] = row[37] &= orow[22];
                    orow[23] = row[36] &= orow[23];
                    orow[24] = row[35] &= orow[24];
                    orow[25] = row[34] &= orow[25];
                    orow[26] = row[33] &= orow[26];
                    orow[27] = row[32] &= orow[27];
                    orow[28] = row[31] &= orow[28];
                    orow[29] = row[30] &= orow[29];
                    orow[30] = row[29] &= orow[30];
                    orow[31] = row[28] &= orow[31];
                    orow[32] = row[27] &= orow[32];
                    orow[33] = row[26] &= orow[33];
                    orow[34] = row[25] &= orow[34];
                    orow[35] = row[24] &= orow[35];
                    orow[36] = row[23] &= orow[36];
                    orow[37] = row[22] &= orow[37];
                    orow[38] = row[21] &= orow[38];
                    orow[39] = row[20] &= orow[39];
                    orow[40] = row[19] &= orow[40];
                    orow[41] = row[18] &= orow[41];
                    orow[42] = row[17] &= orow[42];
                    orow[43] = row[16] &= orow[43];
                    orow[44] = row[15] &= orow[44];
                    orow[45] = row[14] &= orow[45];
                    orow[46] = row[13] &= orow[46];
                    orow[47] = row[12] &= orow[47];
                    orow[48] = row[11] &= orow[48];
                    orow[49] = row[10] &= orow[49];
                    orow[50] = row[9] &= orow[50];
                    orow[51] = row[8] &= orow[51];
                    orow[52] = row[7] &= orow[52];
                    orow[53] = row[6] &= orow[53];
                    orow[54] = row[5] &= orow[54];
                    orow[55] = row[4] &= orow[55];
                    orow[56] = row[3] &= orow[56];
                    orow[57] = row[2] &= orow[57];
                    orow[58] = row[1] &= orow[58];
                    orow[59] = row[0] &= orow[59];
                    break;
                case 59:
                    orow[0] = row[58] &= orow[0];
                    orow[1] = row[57] &= orow[1];
                    orow[2] = row[56] &= orow[2];
                    orow[3] = row[55] &= orow[3];
                    orow[4] = row[54] &= orow[4];
                    orow[5] = row[53] &= orow[5];
                    orow[6] = row[52] &= orow[6];
                    orow[7] = row[51] &= orow[7];
                    orow[8] = row[50] &= orow[8];
                    orow[9] = row[49] &= orow[9];
                    orow[10] = row[48] &= orow[10];
                    orow[11] = row[47] &= orow[11];
                    orow[12] = row[46] &= orow[12];
                    orow[13] = row[45] &= orow[13];
                    orow[14] = row[44] &= orow[14];
                    orow[15] = row[43] &= orow[15];
                    orow[16] = row[42] &= orow[16];
                    orow[17] = row[41] &= orow[17];
                    orow[18] = row[40] &= orow[18];
                    orow[19] = row[39] &= orow[19];
                    orow[20] = row[38] &= orow[20];
                    orow[21] = row[37] &= orow[21];
                    orow[22] = row[36] &= orow[22];
                    orow[23] = row[35] &= orow[23];
                    orow[24] = row[34] &= orow[24];
                    orow[25] = row[33] &= orow[25];
                    orow[26] = row[32] &= orow[26];
                    orow[27] = row[31] &= orow[27];
                    orow[28] = row[30] &= orow[28];
                    orow[29] = row[29] &= orow[29];
                    orow[30] = row[28] &= orow[30];
                    orow[31] = row[27] &= orow[31];
                    orow[32] = row[26] &= orow[32];
                    orow[33] = row[25] &= orow[33];
                    orow[34] = row[24] &= orow[34];
                    orow[35] = row[23] &= orow[35];
                    orow[36] = row[22] &= orow[36];
                    orow[37] = row[21] &= orow[37];
                    orow[38] = row[20] &= orow[38];
                    orow[39] = row[19] &= orow[39];
                    orow[40] = row[18] &= orow[40];
                    orow[41] = row[17] &= orow[41];
                    orow[42] = row[16] &= orow[42];
                    orow[43] = row[15] &= orow[43];
                    orow[44] = row[14] &= orow[44];
                    orow[45] = row[13] &= orow[45];
                    orow[46] = row[12] &= orow[46];
                    orow[47] = row[11] &= orow[47];
                    orow[48] = row[10] &= orow[48];
                    orow[49] = row[9] &= orow[49];
                    orow[50] = row[8] &= orow[50];
                    orow[51] = row[7] &= orow[51];
                    orow[52] = row[6] &= orow[52];
                    orow[53] = row[5] &= orow[53];
                    orow[54] = row[4] &= orow[54];
                    orow[55] = row[3] &= orow[55];
                    orow[56] = row[2] &= orow[56];
                    orow[57] = row[1] &= orow[57];
                    orow[58] = row[0] &= orow[58];
                    break;
                case 58:
                    orow[0] = row[57] &= orow[0];
                    orow[1] = row[56] &= orow[1];
                    orow[2] = row[55] &= orow[2];
                    orow[3] = row[54] &= orow[3];
                    orow[4] = row[53] &= orow[4];
                    orow[5] = row[52] &= orow[5];
                    orow[6] = row[51] &= orow[6];
                    orow[7] = row[50] &= orow[7];
                    orow[8] = row[49] &= orow[8];
                    orow[9] = row[48] &= orow[9];
                    orow[10] = row[47] &= orow[10];
                    orow[11] = row[46] &= orow[11];
                    orow[12] = row[45] &= orow[12];
                    orow[13] = row[44] &= orow[13];
                    orow[14] = row[43] &= orow[14];
                    orow[15] = row[42] &= orow[15];
                    orow[16] = row[41] &= orow[16];
                    orow[17] = row[40] &= orow[17];
                    orow[18] = row[39] &= orow[18];
                    orow[19] = row[38] &= orow[19];
                    orow[20] = row[37] &= orow[20];
                    orow[21] = row[36] &= orow[21];
                    orow[22] = row[35] &= orow[22];
                    orow[23] = row[34] &= orow[23];
                    orow[24] = row[33] &= orow[24];
                    orow[25] = row[32] &= orow[25];
                    orow[26] = row[31] &= orow[26];
                    orow[27] = row[30] &= orow[27];
                    orow[28] = row[29] &= orow[28];
                    orow[29] = row[28] &= orow[29];
                    orow[30] = row[27] &= orow[30];
                    orow[31] = row[26] &= orow[31];
                    orow[32] = row[25] &= orow[32];
                    orow[33] = row[24] &= orow[33];
                    orow[34] = row[23] &= orow[34];
                    orow[35] = row[22] &= orow[35];
                    orow[36] = row[21] &= orow[36];
                    orow[37] = row[20] &= orow[37];
                    orow[38] = row[19] &= orow[38];
                    orow[39] = row[18] &= orow[39];
                    orow[40] = row[17] &= orow[40];
                    orow[41] = row[16] &= orow[41];
                    orow[42] = row[15] &= orow[42];
                    orow[43] = row[14] &= orow[43];
                    orow[44] = row[13] &= orow[44];
                    orow[45] = row[12] &= orow[45];
                    orow[46] = row[11] &= orow[46];
                    orow[47] = row[10] &= orow[47];
                    orow[48] = row[9] &= orow[48];
                    orow[49] = row[8] &= orow[49];
                    orow[50] = row[7] &= orow[50];
                    orow[51] = row[6] &= orow[51];
                    orow[52] = row[5] &= orow[52];
                    orow[53] = row[4] &= orow[53];
                    orow[54] = row[3] &= orow[54];
                    orow[55] = row[2] &= orow[55];
                    orow[56] = row[1] &= orow[56];
                    orow[57] = row[0] &= orow[57];
                    break;
                case 57:
                    orow[0] = row[56] &= orow[0];
                    orow[1] = row[55] &= orow[1];
                    orow[2] = row[54] &= orow[2];
                    orow[3] = row[53] &= orow[3];
                    orow[4] = row[52] &= orow[4];
                    orow[5] = row[51] &= orow[5];
                    orow[6] = row[50] &= orow[6];
                    orow[7] = row[49] &= orow[7];
                    orow[8] = row[48] &= orow[8];
                    orow[9] = row[47] &= orow[9];
                    orow[10] = row[46] &= orow[10];
                    orow[11] = row[45] &= orow[11];
                    orow[12] = row[44] &= orow[12];
                    orow[13] = row[43] &= orow[13];
                    orow[14] = row[42] &= orow[14];
                    orow[15] = row[41] &= orow[15];
                    orow[16] = row[40] &= orow[16];
                    orow[17] = row[39] &= orow[17];
                    orow[18] = row[38] &= orow[18];
                    orow[19] = row[37] &= orow[19];
                    orow[20] = row[36] &= orow[20];
                    orow[21] = row[35] &= orow[21];
                    orow[22] = row[34] &= orow[22];
                    orow[23] = row[33] &= orow[23];
                    orow[24] = row[32] &= orow[24];
                    orow[25] = row[31] &= orow[25];
                    orow[26] = row[30] &= orow[26];
                    orow[27] = row[29] &= orow[27];
                    orow[28] = row[28] &= orow[28];
                    orow[29] = row[27] &= orow[29];
                    orow[30] = row[26] &= orow[30];
                    orow[31] = row[25] &= orow[31];
                    orow[32] = row[24] &= orow[32];
                    orow[33] = row[23] &= orow[33];
                    orow[34] = row[22] &= orow[34];
                    orow[35] = row[21] &= orow[35];
                    orow[36] = row[20] &= orow[36];
                    orow[37] = row[19] &= orow[37];
                    orow[38] = row[18] &= orow[38];
                    orow[39] = row[17] &= orow[39];
                    orow[40] = row[16] &= orow[40];
                    orow[41] = row[15] &= orow[41];
                    orow[42] = row[14] &= orow[42];
                    orow[43] = row[13] &= orow[43];
                    orow[44] = row[12] &= orow[44];
                    orow[45] = row[11] &= orow[45];
                    orow[46] = row[10] &= orow[46];
                    orow[47] = row[9] &= orow[47];
                    orow[48] = row[8] &= orow[48];
                    orow[49] = row[7] &= orow[49];
                    orow[50] = row[6] &= orow[50];
                    orow[51] = row[5] &= orow[51];
                    orow[52] = row[4] &= orow[52];
                    orow[53] = row[3] &= orow[53];
                    orow[54] = row[2] &= orow[54];
                    orow[55] = row[1] &= orow[55];
                    orow[56] = row[0] &= orow[56];
                    break;
                case 56:
                    orow[0] = row[55] &= orow[0];
                    orow[1] = row[54] &= orow[1];
                    orow[2] = row[53] &= orow[2];
                    orow[3] = row[52] &= orow[3];
                    orow[4] = row[51] &= orow[4];
                    orow[5] = row[50] &= orow[5];
                    orow[6] = row[49] &= orow[6];
                    orow[7] = row[48] &= orow[7];
                    orow[8] = row[47] &= orow[8];
                    orow[9] = row[46] &= orow[9];
                    orow[10] = row[45] &= orow[10];
                    orow[11] = row[44] &= orow[11];
                    orow[12] = row[43] &= orow[12];
                    orow[13] = row[42] &= orow[13];
                    orow[14] = row[41] &= orow[14];
                    orow[15] = row[40] &= orow[15];
                    orow[16] = row[39] &= orow[16];
                    orow[17] = row[38] &= orow[17];
                    orow[18] = row[37] &= orow[18];
                    orow[19] = row[36] &= orow[19];
                    orow[20] = row[35] &= orow[20];
                    orow[21] = row[34] &= orow[21];
                    orow[22] = row[33] &= orow[22];
                    orow[23] = row[32] &= orow[23];
                    orow[24] = row[31] &= orow[24];
                    orow[25] = row[30] &= orow[25];
                    orow[26] = row[29] &= orow[26];
                    orow[27] = row[28] &= orow[27];
                    orow[28] = row[27] &= orow[28];
                    orow[29] = row[26] &= orow[29];
                    orow[30] = row[25] &= orow[30];
                    orow[31] = row[24] &= orow[31];
                    orow[32] = row[23] &= orow[32];
                    orow[33] = row[22] &= orow[33];
                    orow[34] = row[21] &= orow[34];
                    orow[35] = row[20] &= orow[35];
                    orow[36] = row[19] &= orow[36];
                    orow[37] = row[18] &= orow[37];
                    orow[38] = row[17] &= orow[38];
                    orow[39] = row[16] &= orow[39];
                    orow[40] = row[15] &= orow[40];
                    orow[41] = row[14] &= orow[41];
                    orow[42] = row[13] &= orow[42];
                    orow[43] = row[12] &= orow[43];
                    orow[44] = row[11] &= orow[44];
                    orow[45] = row[10] &= orow[45];
                    orow[46] = row[9] &= orow[46];
                    orow[47] = row[8] &= orow[47];
                    orow[48] = row[7] &= orow[48];
                    orow[49] = row[6] &= orow[49];
                    orow[50] = row[5] &= orow[50];
                    orow[51] = row[4] &= orow[51];
                    orow[52] = row[3] &= orow[52];
                    orow[53] = row[2] &= orow[53];
                    orow[54] = row[1] &= orow[54];
                    orow[55] = row[0] &= orow[55];
                    break;
                case 55:
                    orow[0] = row[54] &= orow[0];
                    orow[1] = row[53] &= orow[1];
                    orow[2] = row[52] &= orow[2];
                    orow[3] = row[51] &= orow[3];
                    orow[4] = row[50] &= orow[4];
                    orow[5] = row[49] &= orow[5];
                    orow[6] = row[48] &= orow[6];
                    orow[7] = row[47] &= orow[7];
                    orow[8] = row[46] &= orow[8];
                    orow[9] = row[45] &= orow[9];
                    orow[10] = row[44] &= orow[10];
                    orow[11] = row[43] &= orow[11];
                    orow[12] = row[42] &= orow[12];
                    orow[13] = row[41] &= orow[13];
                    orow[14] = row[40] &= orow[14];
                    orow[15] = row[39] &= orow[15];
                    orow[16] = row[38] &= orow[16];
                    orow[17] = row[37] &= orow[17];
                    orow[18] = row[36] &= orow[18];
                    orow[19] = row[35] &= orow[19];
                    orow[20] = row[34] &= orow[20];
                    orow[21] = row[33] &= orow[21];
                    orow[22] = row[32] &= orow[22];
                    orow[23] = row[31] &= orow[23];
                    orow[24] = row[30] &= orow[24];
                    orow[25] = row[29] &= orow[25];
                    orow[26] = row[28] &= orow[26];
                    orow[27] = row[27] &= orow[27];
                    orow[28] = row[26] &= orow[28];
                    orow[29] = row[25] &= orow[29];
                    orow[30] = row[24] &= orow[30];
                    orow[31] = row[23] &= orow[31];
                    orow[32] = row[22] &= orow[32];
                    orow[33] = row[21] &= orow[33];
                    orow[34] = row[20] &= orow[34];
                    orow[35] = row[19] &= orow[35];
                    orow[36] = row[18] &= orow[36];
                    orow[37] = row[17] &= orow[37];
                    orow[38] = row[16] &= orow[38];
                    orow[39] = row[15] &= orow[39];
                    orow[40] = row[14] &= orow[40];
                    orow[41] = row[13] &= orow[41];
                    orow[42] = row[12] &= orow[42];
                    orow[43] = row[11] &= orow[43];
                    orow[44] = row[10] &= orow[44];
                    orow[45] = row[9] &= orow[45];
                    orow[46] = row[8] &= orow[46];
                    orow[47] = row[7] &= orow[47];
                    orow[48] = row[6] &= orow[48];
                    orow[49] = row[5] &= orow[49];
                    orow[50] = row[4] &= orow[50];
                    orow[51] = row[3] &= orow[51];
                    orow[52] = row[2] &= orow[52];
                    orow[53] = row[1] &= orow[53];
                    orow[54] = row[0] &= orow[54];
                    break;
                case 54:
                    orow[0] = row[53] &= orow[0];
                    orow[1] = row[52] &= orow[1];
                    orow[2] = row[51] &= orow[2];
                    orow[3] = row[50] &= orow[3];
                    orow[4] = row[49] &= orow[4];
                    orow[5] = row[48] &= orow[5];
                    orow[6] = row[47] &= orow[6];
                    orow[7] = row[46] &= orow[7];
                    orow[8] = row[45] &= orow[8];
                    orow[9] = row[44] &= orow[9];
                    orow[10] = row[43] &= orow[10];
                    orow[11] = row[42] &= orow[11];
                    orow[12] = row[41] &= orow[12];
                    orow[13] = row[40] &= orow[13];
                    orow[14] = row[39] &= orow[14];
                    orow[15] = row[38] &= orow[15];
                    orow[16] = row[37] &= orow[16];
                    orow[17] = row[36] &= orow[17];
                    orow[18] = row[35] &= orow[18];
                    orow[19] = row[34] &= orow[19];
                    orow[20] = row[33] &= orow[20];
                    orow[21] = row[32] &= orow[21];
                    orow[22] = row[31] &= orow[22];
                    orow[23] = row[30] &= orow[23];
                    orow[24] = row[29] &= orow[24];
                    orow[25] = row[28] &= orow[25];
                    orow[26] = row[27] &= orow[26];
                    orow[27] = row[26] &= orow[27];
                    orow[28] = row[25] &= orow[28];
                    orow[29] = row[24] &= orow[29];
                    orow[30] = row[23] &= orow[30];
                    orow[31] = row[22] &= orow[31];
                    orow[32] = row[21] &= orow[32];
                    orow[33] = row[20] &= orow[33];
                    orow[34] = row[19] &= orow[34];
                    orow[35] = row[18] &= orow[35];
                    orow[36] = row[17] &= orow[36];
                    orow[37] = row[16] &= orow[37];
                    orow[38] = row[15] &= orow[38];
                    orow[39] = row[14] &= orow[39];
                    orow[40] = row[13] &= orow[40];
                    orow[41] = row[12] &= orow[41];
                    orow[42] = row[11] &= orow[42];
                    orow[43] = row[10] &= orow[43];
                    orow[44] = row[9] &= orow[44];
                    orow[45] = row[8] &= orow[45];
                    orow[46] = row[7] &= orow[46];
                    orow[47] = row[6] &= orow[47];
                    orow[48] = row[5] &= orow[48];
                    orow[49] = row[4] &= orow[49];
                    orow[50] = row[3] &= orow[50];
                    orow[51] = row[2] &= orow[51];
                    orow[52] = row[1] &= orow[52];
                    orow[53] = row[0] &= orow[53];
                    break;
                case 53:
                    orow[0] = row[52] &= orow[0];
                    orow[1] = row[51] &= orow[1];
                    orow[2] = row[50] &= orow[2];
                    orow[3] = row[49] &= orow[3];
                    orow[4] = row[48] &= orow[4];
                    orow[5] = row[47] &= orow[5];
                    orow[6] = row[46] &= orow[6];
                    orow[7] = row[45] &= orow[7];
                    orow[8] = row[44] &= orow[8];
                    orow[9] = row[43] &= orow[9];
                    orow[10] = row[42] &= orow[10];
                    orow[11] = row[41] &= orow[11];
                    orow[12] = row[40] &= orow[12];
                    orow[13] = row[39] &= orow[13];
                    orow[14] = row[38] &= orow[14];
                    orow[15] = row[37] &= orow[15];
                    orow[16] = row[36] &= orow[16];
                    orow[17] = row[35] &= orow[17];
                    orow[18] = row[34] &= orow[18];
                    orow[19] = row[33] &= orow[19];
                    orow[20] = row[32] &= orow[20];
                    orow[21] = row[31] &= orow[21];
                    orow[22] = row[30] &= orow[22];
                    orow[23] = row[29] &= orow[23];
                    orow[24] = row[28] &= orow[24];
                    orow[25] = row[27] &= orow[25];
                    orow[26] = row[26] &= orow[26];
                    orow[27] = row[25] &= orow[27];
                    orow[28] = row[24] &= orow[28];
                    orow[29] = row[23] &= orow[29];
                    orow[30] = row[22] &= orow[30];
                    orow[31] = row[21] &= orow[31];
                    orow[32] = row[20] &= orow[32];
                    orow[33] = row[19] &= orow[33];
                    orow[34] = row[18] &= orow[34];
                    orow[35] = row[17] &= orow[35];
                    orow[36] = row[16] &= orow[36];
                    orow[37] = row[15] &= orow[37];
                    orow[38] = row[14] &= orow[38];
                    orow[39] = row[13] &= orow[39];
                    orow[40] = row[12] &= orow[40];
                    orow[41] = row[11] &= orow[41];
                    orow[42] = row[10] &= orow[42];
                    orow[43] = row[9] &= orow[43];
                    orow[44] = row[8] &= orow[44];
                    orow[45] = row[7] &= orow[45];
                    orow[46] = row[6] &= orow[46];
                    orow[47] = row[5] &= orow[47];
                    orow[48] = row[4] &= orow[48];
                    orow[49] = row[3] &= orow[49];
                    orow[50] = row[2] &= orow[50];
                    orow[51] = row[1] &= orow[51];
                    orow[52] = row[0] &= orow[52];
                    break;
                case 52:
                    orow[0] = row[51] &= orow[0];
                    orow[1] = row[50] &= orow[1];
                    orow[2] = row[49] &= orow[2];
                    orow[3] = row[48] &= orow[3];
                    orow[4] = row[47] &= orow[4];
                    orow[5] = row[46] &= orow[5];
                    orow[6] = row[45] &= orow[6];
                    orow[7] = row[44] &= orow[7];
                    orow[8] = row[43] &= orow[8];
                    orow[9] = row[42] &= orow[9];
                    orow[10] = row[41] &= orow[10];
                    orow[11] = row[40] &= orow[11];
                    orow[12] = row[39] &= orow[12];
                    orow[13] = row[38] &= orow[13];
                    orow[14] = row[37] &= orow[14];
                    orow[15] = row[36] &= orow[15];
                    orow[16] = row[35] &= orow[16];
                    orow[17] = row[34] &= orow[17];
                    orow[18] = row[33] &= orow[18];
                    orow[19] = row[32] &= orow[19];
                    orow[20] = row[31] &= orow[20];
                    orow[21] = row[30] &= orow[21];
                    orow[22] = row[29] &= orow[22];
                    orow[23] = row[28] &= orow[23];
                    orow[24] = row[27] &= orow[24];
                    orow[25] = row[26] &= orow[25];
                    orow[26] = row[25] &= orow[26];
                    orow[27] = row[24] &= orow[27];
                    orow[28] = row[23] &= orow[28];
                    orow[29] = row[22] &= orow[29];
                    orow[30] = row[21] &= orow[30];
                    orow[31] = row[20] &= orow[31];
                    orow[32] = row[19] &= orow[32];
                    orow[33] = row[18] &= orow[33];
                    orow[34] = row[17] &= orow[34];
                    orow[35] = row[16] &= orow[35];
                    orow[36] = row[15] &= orow[36];
                    orow[37] = row[14] &= orow[37];
                    orow[38] = row[13] &= orow[38];
                    orow[39] = row[12] &= orow[39];
                    orow[40] = row[11] &= orow[40];
                    orow[41] = row[10] &= orow[41];
                    orow[42] = row[9] &= orow[42];
                    orow[43] = row[8] &= orow[43];
                    orow[44] = row[7] &= orow[44];
                    orow[45] = row[6] &= orow[45];
                    orow[46] = row[5] &= orow[46];
                    orow[47] = row[4] &= orow[47];
                    orow[48] = row[3] &= orow[48];
                    orow[49] = row[2] &= orow[49];
                    orow[50] = row[1] &= orow[50];
                    orow[51] = row[0] &= orow[51];
                    break;
                case 51:
                    orow[0] = row[50] &= orow[0];
                    orow[1] = row[49] &= orow[1];
                    orow[2] = row[48] &= orow[2];
                    orow[3] = row[47] &= orow[3];
                    orow[4] = row[46] &= orow[4];
                    orow[5] = row[45] &= orow[5];
                    orow[6] = row[44] &= orow[6];
                    orow[7] = row[43] &= orow[7];
                    orow[8] = row[42] &= orow[8];
                    orow[9] = row[41] &= orow[9];
                    orow[10] = row[40] &= orow[10];
                    orow[11] = row[39] &= orow[11];
                    orow[12] = row[38] &= orow[12];
                    orow[13] = row[37] &= orow[13];
                    orow[14] = row[36] &= orow[14];
                    orow[15] = row[35] &= orow[15];
                    orow[16] = row[34] &= orow[16];
                    orow[17] = row[33] &= orow[17];
                    orow[18] = row[32] &= orow[18];
                    orow[19] = row[31] &= orow[19];
                    orow[20] = row[30] &= orow[20];
                    orow[21] = row[29] &= orow[21];
                    orow[22] = row[28] &= orow[22];
                    orow[23] = row[27] &= orow[23];
                    orow[24] = row[26] &= orow[24];
                    orow[25] = row[25] &= orow[25];
                    orow[26] = row[24] &= orow[26];
                    orow[27] = row[23] &= orow[27];
                    orow[28] = row[22] &= orow[28];
                    orow[29] = row[21] &= orow[29];
                    orow[30] = row[20] &= orow[30];
                    orow[31] = row[19] &= orow[31];
                    orow[32] = row[18] &= orow[32];
                    orow[33] = row[17] &= orow[33];
                    orow[34] = row[16] &= orow[34];
                    orow[35] = row[15] &= orow[35];
                    orow[36] = row[14] &= orow[36];
                    orow[37] = row[13] &= orow[37];
                    orow[38] = row[12] &= orow[38];
                    orow[39] = row[11] &= orow[39];
                    orow[40] = row[10] &= orow[40];
                    orow[41] = row[9] &= orow[41];
                    orow[42] = row[8] &= orow[42];
                    orow[43] = row[7] &= orow[43];
                    orow[44] = row[6] &= orow[44];
                    orow[45] = row[5] &= orow[45];
                    orow[46] = row[4] &= orow[46];
                    orow[47] = row[3] &= orow[47];
                    orow[48] = row[2] &= orow[48];
                    orow[49] = row[1] &= orow[49];
                    orow[50] = row[0] &= orow[50];
                    break;
                case 50:
                    orow[0] = row[49] &= orow[0];
                    orow[1] = row[48] &= orow[1];
                    orow[2] = row[47] &= orow[2];
                    orow[3] = row[46] &= orow[3];
                    orow[4] = row[45] &= orow[4];
                    orow[5] = row[44] &= orow[5];
                    orow[6] = row[43] &= orow[6];
                    orow[7] = row[42] &= orow[7];
                    orow[8] = row[41] &= orow[8];
                    orow[9] = row[40] &= orow[9];
                    orow[10] = row[39] &= orow[10];
                    orow[11] = row[38] &= orow[11];
                    orow[12] = row[37] &= orow[12];
                    orow[13] = row[36] &= orow[13];
                    orow[14] = row[35] &= orow[14];
                    orow[15] = row[34] &= orow[15];
                    orow[16] = row[33] &= orow[16];
                    orow[17] = row[32] &= orow[17];
                    orow[18] = row[31] &= orow[18];
                    orow[19] = row[30] &= orow[19];
                    orow[20] = row[29] &= orow[20];
                    orow[21] = row[28] &= orow[21];
                    orow[22] = row[27] &= orow[22];
                    orow[23] = row[26] &= orow[23];
                    orow[24] = row[25] &= orow[24];
                    orow[25] = row[24] &= orow[25];
                    orow[26] = row[23] &= orow[26];
                    orow[27] = row[22] &= orow[27];
                    orow[28] = row[21] &= orow[28];
                    orow[29] = row[20] &= orow[29];
                    orow[30] = row[19] &= orow[30];
                    orow[31] = row[18] &= orow[31];
                    orow[32] = row[17] &= orow[32];
                    orow[33] = row[16] &= orow[33];
                    orow[34] = row[15] &= orow[34];
                    orow[35] = row[14] &= orow[35];
                    orow[36] = row[13] &= orow[36];
                    orow[37] = row[12] &= orow[37];
                    orow[38] = row[11] &= orow[38];
                    orow[39] = row[10] &= orow[39];
                    orow[40] = row[9] &= orow[40];
                    orow[41] = row[8] &= orow[41];
                    orow[42] = row[7] &= orow[42];
                    orow[43] = row[6] &= orow[43];
                    orow[44] = row[5] &= orow[44];
                    orow[45] = row[4] &= orow[45];
                    orow[46] = row[3] &= orow[46];
                    orow[47] = row[2] &= orow[47];
                    orow[48] = row[1] &= orow[48];
                    orow[49] = row[0] &= orow[49];
                    break;
                case 49:
                    orow[0] = row[48] &= orow[0];
                    orow[1] = row[47] &= orow[1];
                    orow[2] = row[46] &= orow[2];
                    orow[3] = row[45] &= orow[3];
                    orow[4] = row[44] &= orow[4];
                    orow[5] = row[43] &= orow[5];
                    orow[6] = row[42] &= orow[6];
                    orow[7] = row[41] &= orow[7];
                    orow[8] = row[40] &= orow[8];
                    orow[9] = row[39] &= orow[9];
                    orow[10] = row[38] &= orow[10];
                    orow[11] = row[37] &= orow[11];
                    orow[12] = row[36] &= orow[12];
                    orow[13] = row[35] &= orow[13];
                    orow[14] = row[34] &= orow[14];
                    orow[15] = row[33] &= orow[15];
                    orow[16] = row[32] &= orow[16];
                    orow[17] = row[31] &= orow[17];
                    orow[18] = row[30] &= orow[18];
                    orow[19] = row[29] &= orow[19];
                    orow[20] = row[28] &= orow[20];
                    orow[21] = row[27] &= orow[21];
                    orow[22] = row[26] &= orow[22];
                    orow[23] = row[25] &= orow[23];
                    orow[24] = row[24] &= orow[24];
                    orow[25] = row[23] &= orow[25];
                    orow[26] = row[22] &= orow[26];
                    orow[27] = row[21] &= orow[27];
                    orow[28] = row[20] &= orow[28];
                    orow[29] = row[19] &= orow[29];
                    orow[30] = row[18] &= orow[30];
                    orow[31] = row[17] &= orow[31];
                    orow[32] = row[16] &= orow[32];
                    orow[33] = row[15] &= orow[33];
                    orow[34] = row[14] &= orow[34];
                    orow[35] = row[13] &= orow[35];
                    orow[36] = row[12] &= orow[36];
                    orow[37] = row[11] &= orow[37];
                    orow[38] = row[10] &= orow[38];
                    orow[39] = row[9] &= orow[39];
                    orow[40] = row[8] &= orow[40];
                    orow[41] = row[7] &= orow[41];
                    orow[42] = row[6] &= orow[42];
                    orow[43] = row[5] &= orow[43];
                    orow[44] = row[4] &= orow[44];
                    orow[45] = row[3] &= orow[45];
                    orow[46] = row[2] &= orow[46];
                    orow[47] = row[1] &= orow[47];
                    orow[48] = row[0] &= orow[48];
                    break;
                case 48:
                    orow[0] = row[47] &= orow[0];
                    orow[1] = row[46] &= orow[1];
                    orow[2] = row[45] &= orow[2];
                    orow[3] = row[44] &= orow[3];
                    orow[4] = row[43] &= orow[4];
                    orow[5] = row[42] &= orow[5];
                    orow[6] = row[41] &= orow[6];
                    orow[7] = row[40] &= orow[7];
                    orow[8] = row[39] &= orow[8];
                    orow[9] = row[38] &= orow[9];
                    orow[10] = row[37] &= orow[10];
                    orow[11] = row[36] &= orow[11];
                    orow[12] = row[35] &= orow[12];
                    orow[13] = row[34] &= orow[13];
                    orow[14] = row[33] &= orow[14];
                    orow[15] = row[32] &= orow[15];
                    orow[16] = row[31] &= orow[16];
                    orow[17] = row[30] &= orow[17];
                    orow[18] = row[29] &= orow[18];
                    orow[19] = row[28] &= orow[19];
                    orow[20] = row[27] &= orow[20];
                    orow[21] = row[26] &= orow[21];
                    orow[22] = row[25] &= orow[22];
                    orow[23] = row[24] &= orow[23];
                    orow[24] = row[23] &= orow[24];
                    orow[25] = row[22] &= orow[25];
                    orow[26] = row[21] &= orow[26];
                    orow[27] = row[20] &= orow[27];
                    orow[28] = row[19] &= orow[28];
                    orow[29] = row[18] &= orow[29];
                    orow[30] = row[17] &= orow[30];
                    orow[31] = row[16] &= orow[31];
                    orow[32] = row[15] &= orow[32];
                    orow[33] = row[14] &= orow[33];
                    orow[34] = row[13] &= orow[34];
                    orow[35] = row[12] &= orow[35];
                    orow[36] = row[11] &= orow[36];
                    orow[37] = row[10] &= orow[37];
                    orow[38] = row[9] &= orow[38];
                    orow[39] = row[8] &= orow[39];
                    orow[40] = row[7] &= orow[40];
                    orow[41] = row[6] &= orow[41];
                    orow[42] = row[5] &= orow[42];
                    orow[43] = row[4] &= orow[43];
                    orow[44] = row[3] &= orow[44];
                    orow[45] = row[2] &= orow[45];
                    orow[46] = row[1] &= orow[46];
                    orow[47] = row[0] &= orow[47];
                    break;
                case 47:
                    orow[0] = row[46] &= orow[0];
                    orow[1] = row[45] &= orow[1];
                    orow[2] = row[44] &= orow[2];
                    orow[3] = row[43] &= orow[3];
                    orow[4] = row[42] &= orow[4];
                    orow[5] = row[41] &= orow[5];
                    orow[6] = row[40] &= orow[6];
                    orow[7] = row[39] &= orow[7];
                    orow[8] = row[38] &= orow[8];
                    orow[9] = row[37] &= orow[9];
                    orow[10] = row[36] &= orow[10];
                    orow[11] = row[35] &= orow[11];
                    orow[12] = row[34] &= orow[12];
                    orow[13] = row[33] &= orow[13];
                    orow[14] = row[32] &= orow[14];
                    orow[15] = row[31] &= orow[15];
                    orow[16] = row[30] &= orow[16];
                    orow[17] = row[29] &= orow[17];
                    orow[18] = row[28] &= orow[18];
                    orow[19] = row[27] &= orow[19];
                    orow[20] = row[26] &= orow[20];
                    orow[21] = row[25] &= orow[21];
                    orow[22] = row[24] &= orow[22];
                    orow[23] = row[23] &= orow[23];
                    orow[24] = row[22] &= orow[24];
                    orow[25] = row[21] &= orow[25];
                    orow[26] = row[20] &= orow[26];
                    orow[27] = row[19] &= orow[27];
                    orow[28] = row[18] &= orow[28];
                    orow[29] = row[17] &= orow[29];
                    orow[30] = row[16] &= orow[30];
                    orow[31] = row[15] &= orow[31];
                    orow[32] = row[14] &= orow[32];
                    orow[33] = row[13] &= orow[33];
                    orow[34] = row[12] &= orow[34];
                    orow[35] = row[11] &= orow[35];
                    orow[36] = row[10] &= orow[36];
                    orow[37] = row[9] &= orow[37];
                    orow[38] = row[8] &= orow[38];
                    orow[39] = row[7] &= orow[39];
                    orow[40] = row[6] &= orow[40];
                    orow[41] = row[5] &= orow[41];
                    orow[42] = row[4] &= orow[42];
                    orow[43] = row[3] &= orow[43];
                    orow[44] = row[2] &= orow[44];
                    orow[45] = row[1] &= orow[45];
                    orow[46] = row[0] &= orow[46];
                    break;
                case 46:
                    orow[0] = row[45] &= orow[0];
                    orow[1] = row[44] &= orow[1];
                    orow[2] = row[43] &= orow[2];
                    orow[3] = row[42] &= orow[3];
                    orow[4] = row[41] &= orow[4];
                    orow[5] = row[40] &= orow[5];
                    orow[6] = row[39] &= orow[6];
                    orow[7] = row[38] &= orow[7];
                    orow[8] = row[37] &= orow[8];
                    orow[9] = row[36] &= orow[9];
                    orow[10] = row[35] &= orow[10];
                    orow[11] = row[34] &= orow[11];
                    orow[12] = row[33] &= orow[12];
                    orow[13] = row[32] &= orow[13];
                    orow[14] = row[31] &= orow[14];
                    orow[15] = row[30] &= orow[15];
                    orow[16] = row[29] &= orow[16];
                    orow[17] = row[28] &= orow[17];
                    orow[18] = row[27] &= orow[18];
                    orow[19] = row[26] &= orow[19];
                    orow[20] = row[25] &= orow[20];
                    orow[21] = row[24] &= orow[21];
                    orow[22] = row[23] &= orow[22];
                    orow[23] = row[22] &= orow[23];
                    orow[24] = row[21] &= orow[24];
                    orow[25] = row[20] &= orow[25];
                    orow[26] = row[19] &= orow[26];
                    orow[27] = row[18] &= orow[27];
                    orow[28] = row[17] &= orow[28];
                    orow[29] = row[16] &= orow[29];
                    orow[30] = row[15] &= orow[30];
                    orow[31] = row[14] &= orow[31];
                    orow[32] = row[13] &= orow[32];
                    orow[33] = row[12] &= orow[33];
                    orow[34] = row[11] &= orow[34];
                    orow[35] = row[10] &= orow[35];
                    orow[36] = row[9] &= orow[36];
                    orow[37] = row[8] &= orow[37];
                    orow[38] = row[7] &= orow[38];
                    orow[39] = row[6] &= orow[39];
                    orow[40] = row[5] &= orow[40];
                    orow[41] = row[4] &= orow[41];
                    orow[42] = row[3] &= orow[42];
                    orow[43] = row[2] &= orow[43];
                    orow[44] = row[1] &= orow[44];
                    orow[45] = row[0] &= orow[45];
                    break;
                case 45:
                    orow[0] = row[44] &= orow[0];
                    orow[1] = row[43] &= orow[1];
                    orow[2] = row[42] &= orow[2];
                    orow[3] = row[41] &= orow[3];
                    orow[4] = row[40] &= orow[4];
                    orow[5] = row[39] &= orow[5];
                    orow[6] = row[38] &= orow[6];
                    orow[7] = row[37] &= orow[7];
                    orow[8] = row[36] &= orow[8];
                    orow[9] = row[35] &= orow[9];
                    orow[10] = row[34] &= orow[10];
                    orow[11] = row[33] &= orow[11];
                    orow[12] = row[32] &= orow[12];
                    orow[13] = row[31] &= orow[13];
                    orow[14] = row[30] &= orow[14];
                    orow[15] = row[29] &= orow[15];
                    orow[16] = row[28] &= orow[16];
                    orow[17] = row[27] &= orow[17];
                    orow[18] = row[26] &= orow[18];
                    orow[19] = row[25] &= orow[19];
                    orow[20] = row[24] &= orow[20];
                    orow[21] = row[23] &= orow[21];
                    orow[22] = row[22] &= orow[22];
                    orow[23] = row[21] &= orow[23];
                    orow[24] = row[20] &= orow[24];
                    orow[25] = row[19] &= orow[25];
                    orow[26] = row[18] &= orow[26];
                    orow[27] = row[17] &= orow[27];
                    orow[28] = row[16] &= orow[28];
                    orow[29] = row[15] &= orow[29];
                    orow[30] = row[14] &= orow[30];
                    orow[31] = row[13] &= orow[31];
                    orow[32] = row[12] &= orow[32];
                    orow[33] = row[11] &= orow[33];
                    orow[34] = row[10] &= orow[34];
                    orow[35] = row[9] &= orow[35];
                    orow[36] = row[8] &= orow[36];
                    orow[37] = row[7] &= orow[37];
                    orow[38] = row[6] &= orow[38];
                    orow[39] = row[5] &= orow[39];
                    orow[40] = row[4] &= orow[40];
                    orow[41] = row[3] &= orow[41];
                    orow[42] = row[2] &= orow[42];
                    orow[43] = row[1] &= orow[43];
                    orow[44] = row[0] &= orow[44];
                    break;
                case 44:
                    orow[0] = row[43] &= orow[0];
                    orow[1] = row[42] &= orow[1];
                    orow[2] = row[41] &= orow[2];
                    orow[3] = row[40] &= orow[3];
                    orow[4] = row[39] &= orow[4];
                    orow[5] = row[38] &= orow[5];
                    orow[6] = row[37] &= orow[6];
                    orow[7] = row[36] &= orow[7];
                    orow[8] = row[35] &= orow[8];
                    orow[9] = row[34] &= orow[9];
                    orow[10] = row[33] &= orow[10];
                    orow[11] = row[32] &= orow[11];
                    orow[12] = row[31] &= orow[12];
                    orow[13] = row[30] &= orow[13];
                    orow[14] = row[29] &= orow[14];
                    orow[15] = row[28] &= orow[15];
                    orow[16] = row[27] &= orow[16];
                    orow[17] = row[26] &= orow[17];
                    orow[18] = row[25] &= orow[18];
                    orow[19] = row[24] &= orow[19];
                    orow[20] = row[23] &= orow[20];
                    orow[21] = row[22] &= orow[21];
                    orow[22] = row[21] &= orow[22];
                    orow[23] = row[20] &= orow[23];
                    orow[24] = row[19] &= orow[24];
                    orow[25] = row[18] &= orow[25];
                    orow[26] = row[17] &= orow[26];
                    orow[27] = row[16] &= orow[27];
                    orow[28] = row[15] &= orow[28];
                    orow[29] = row[14] &= orow[29];
                    orow[30] = row[13] &= orow[30];
                    orow[31] = row[12] &= orow[31];
                    orow[32] = row[11] &= orow[32];
                    orow[33] = row[10] &= orow[33];
                    orow[34] = row[9] &= orow[34];
                    orow[35] = row[8] &= orow[35];
                    orow[36] = row[7] &= orow[36];
                    orow[37] = row[6] &= orow[37];
                    orow[38] = row[5] &= orow[38];
                    orow[39] = row[4] &= orow[39];
                    orow[40] = row[3] &= orow[40];
                    orow[41] = row[2] &= orow[41];
                    orow[42] = row[1] &= orow[42];
                    orow[43] = row[0] &= orow[43];
                    break;
                case 43:
                    orow[0] = row[42] &= orow[0];
                    orow[1] = row[41] &= orow[1];
                    orow[2] = row[40] &= orow[2];
                    orow[3] = row[39] &= orow[3];
                    orow[4] = row[38] &= orow[4];
                    orow[5] = row[37] &= orow[5];
                    orow[6] = row[36] &= orow[6];
                    orow[7] = row[35] &= orow[7];
                    orow[8] = row[34] &= orow[8];
                    orow[9] = row[33] &= orow[9];
                    orow[10] = row[32] &= orow[10];
                    orow[11] = row[31] &= orow[11];
                    orow[12] = row[30] &= orow[12];
                    orow[13] = row[29] &= orow[13];
                    orow[14] = row[28] &= orow[14];
                    orow[15] = row[27] &= orow[15];
                    orow[16] = row[26] &= orow[16];
                    orow[17] = row[25] &= orow[17];
                    orow[18] = row[24] &= orow[18];
                    orow[19] = row[23] &= orow[19];
                    orow[20] = row[22] &= orow[20];
                    orow[21] = row[21] &= orow[21];
                    orow[22] = row[20] &= orow[22];
                    orow[23] = row[19] &= orow[23];
                    orow[24] = row[18] &= orow[24];
                    orow[25] = row[17] &= orow[25];
                    orow[26] = row[16] &= orow[26];
                    orow[27] = row[15] &= orow[27];
                    orow[28] = row[14] &= orow[28];
                    orow[29] = row[13] &= orow[29];
                    orow[30] = row[12] &= orow[30];
                    orow[31] = row[11] &= orow[31];
                    orow[32] = row[10] &= orow[32];
                    orow[33] = row[9] &= orow[33];
                    orow[34] = row[8] &= orow[34];
                    orow[35] = row[7] &= orow[35];
                    orow[36] = row[6] &= orow[36];
                    orow[37] = row[5] &= orow[37];
                    orow[38] = row[4] &= orow[38];
                    orow[39] = row[3] &= orow[39];
                    orow[40] = row[2] &= orow[40];
                    orow[41] = row[1] &= orow[41];
                    orow[42] = row[0] &= orow[42];
                    break;
                case 42:
                    orow[0] = row[41] &= orow[0];
                    orow[1] = row[40] &= orow[1];
                    orow[2] = row[39] &= orow[2];
                    orow[3] = row[38] &= orow[3];
                    orow[4] = row[37] &= orow[4];
                    orow[5] = row[36] &= orow[5];
                    orow[6] = row[35] &= orow[6];
                    orow[7] = row[34] &= orow[7];
                    orow[8] = row[33] &= orow[8];
                    orow[9] = row[32] &= orow[9];
                    orow[10] = row[31] &= orow[10];
                    orow[11] = row[30] &= orow[11];
                    orow[12] = row[29] &= orow[12];
                    orow[13] = row[28] &= orow[13];
                    orow[14] = row[27] &= orow[14];
                    orow[15] = row[26] &= orow[15];
                    orow[16] = row[25] &= orow[16];
                    orow[17] = row[24] &= orow[17];
                    orow[18] = row[23] &= orow[18];
                    orow[19] = row[22] &= orow[19];
                    orow[20] = row[21] &= orow[20];
                    orow[21] = row[20] &= orow[21];
                    orow[22] = row[19] &= orow[22];
                    orow[23] = row[18] &= orow[23];
                    orow[24] = row[17] &= orow[24];
                    orow[25] = row[16] &= orow[25];
                    orow[26] = row[15] &= orow[26];
                    orow[27] = row[14] &= orow[27];
                    orow[28] = row[13] &= orow[28];
                    orow[29] = row[12] &= orow[29];
                    orow[30] = row[11] &= orow[30];
                    orow[31] = row[10] &= orow[31];
                    orow[32] = row[9] &= orow[32];
                    orow[33] = row[8] &= orow[33];
                    orow[34] = row[7] &= orow[34];
                    orow[35] = row[6] &= orow[35];
                    orow[36] = row[5] &= orow[36];
                    orow[37] = row[4] &= orow[37];
                    orow[38] = row[3] &= orow[38];
                    orow[39] = row[2] &= orow[39];
                    orow[40] = row[1] &= orow[40];
                    orow[41] = row[0] &= orow[41];
                    break;
                case 41:
                    orow[0] = row[40] &= orow[0];
                    orow[1] = row[39] &= orow[1];
                    orow[2] = row[38] &= orow[2];
                    orow[3] = row[37] &= orow[3];
                    orow[4] = row[36] &= orow[4];
                    orow[5] = row[35] &= orow[5];
                    orow[6] = row[34] &= orow[6];
                    orow[7] = row[33] &= orow[7];
                    orow[8] = row[32] &= orow[8];
                    orow[9] = row[31] &= orow[9];
                    orow[10] = row[30] &= orow[10];
                    orow[11] = row[29] &= orow[11];
                    orow[12] = row[28] &= orow[12];
                    orow[13] = row[27] &= orow[13];
                    orow[14] = row[26] &= orow[14];
                    orow[15] = row[25] &= orow[15];
                    orow[16] = row[24] &= orow[16];
                    orow[17] = row[23] &= orow[17];
                    orow[18] = row[22] &= orow[18];
                    orow[19] = row[21] &= orow[19];
                    orow[20] = row[20] &= orow[20];
                    orow[21] = row[19] &= orow[21];
                    orow[22] = row[18] &= orow[22];
                    orow[23] = row[17] &= orow[23];
                    orow[24] = row[16] &= orow[24];
                    orow[25] = row[15] &= orow[25];
                    orow[26] = row[14] &= orow[26];
                    orow[27] = row[13] &= orow[27];
                    orow[28] = row[12] &= orow[28];
                    orow[29] = row[11] &= orow[29];
                    orow[30] = row[10] &= orow[30];
                    orow[31] = row[9] &= orow[31];
                    orow[32] = row[8] &= orow[32];
                    orow[33] = row[7] &= orow[33];
                    orow[34] = row[6] &= orow[34];
                    orow[35] = row[5] &= orow[35];
                    orow[36] = row[4] &= orow[36];
                    orow[37] = row[3] &= orow[37];
                    orow[38] = row[2] &= orow[38];
                    orow[39] = row[1] &= orow[39];
                    orow[40] = row[0] &= orow[40];
                    break;
                case 40:
                    orow[0] = row[39] &= orow[0];
                    orow[1] = row[38] &= orow[1];
                    orow[2] = row[37] &= orow[2];
                    orow[3] = row[36] &= orow[3];
                    orow[4] = row[35] &= orow[4];
                    orow[5] = row[34] &= orow[5];
                    orow[6] = row[33] &= orow[6];
                    orow[7] = row[32] &= orow[7];
                    orow[8] = row[31] &= orow[8];
                    orow[9] = row[30] &= orow[9];
                    orow[10] = row[29] &= orow[10];
                    orow[11] = row[28] &= orow[11];
                    orow[12] = row[27] &= orow[12];
                    orow[13] = row[26] &= orow[13];
                    orow[14] = row[25] &= orow[14];
                    orow[15] = row[24] &= orow[15];
                    orow[16] = row[23] &= orow[16];
                    orow[17] = row[22] &= orow[17];
                    orow[18] = row[21] &= orow[18];
                    orow[19] = row[20] &= orow[19];
                    orow[20] = row[19] &= orow[20];
                    orow[21] = row[18] &= orow[21];
                    orow[22] = row[17] &= orow[22];
                    orow[23] = row[16] &= orow[23];
                    orow[24] = row[15] &= orow[24];
                    orow[25] = row[14] &= orow[25];
                    orow[26] = row[13] &= orow[26];
                    orow[27] = row[12] &= orow[27];
                    orow[28] = row[11] &= orow[28];
                    orow[29] = row[10] &= orow[29];
                    orow[30] = row[9] &= orow[30];
                    orow[31] = row[8] &= orow[31];
                    orow[32] = row[7] &= orow[32];
                    orow[33] = row[6] &= orow[33];
                    orow[34] = row[5] &= orow[34];
                    orow[35] = row[4] &= orow[35];
                    orow[36] = row[3] &= orow[36];
                    orow[37] = row[2] &= orow[37];
                    orow[38] = row[1] &= orow[38];
                    orow[39] = row[0] &= orow[39];
                    break;
                case 39:
                    orow[0] = row[38] &= orow[0];
                    orow[1] = row[37] &= orow[1];
                    orow[2] = row[36] &= orow[2];
                    orow[3] = row[35] &= orow[3];
                    orow[4] = row[34] &= orow[4];
                    orow[5] = row[33] &= orow[5];
                    orow[6] = row[32] &= orow[6];
                    orow[7] = row[31] &= orow[7];
                    orow[8] = row[30] &= orow[8];
                    orow[9] = row[29] &= orow[9];
                    orow[10] = row[28] &= orow[10];
                    orow[11] = row[27] &= orow[11];
                    orow[12] = row[26] &= orow[12];
                    orow[13] = row[25] &= orow[13];
                    orow[14] = row[24] &= orow[14];
                    orow[15] = row[23] &= orow[15];
                    orow[16] = row[22] &= orow[16];
                    orow[17] = row[21] &= orow[17];
                    orow[18] = row[20] &= orow[18];
                    orow[19] = row[19] &= orow[19];
                    orow[20] = row[18] &= orow[20];
                    orow[21] = row[17] &= orow[21];
                    orow[22] = row[16] &= orow[22];
                    orow[23] = row[15] &= orow[23];
                    orow[24] = row[14] &= orow[24];
                    orow[25] = row[13] &= orow[25];
                    orow[26] = row[12] &= orow[26];
                    orow[27] = row[11] &= orow[27];
                    orow[28] = row[10] &= orow[28];
                    orow[29] = row[9] &= orow[29];
                    orow[30] = row[8] &= orow[30];
                    orow[31] = row[7] &= orow[31];
                    orow[32] = row[6] &= orow[32];
                    orow[33] = row[5] &= orow[33];
                    orow[34] = row[4] &= orow[34];
                    orow[35] = row[3] &= orow[35];
                    orow[36] = row[2] &= orow[36];
                    orow[37] = row[1] &= orow[37];
                    orow[38] = row[0] &= orow[38];
                    break;
                case 38:
                    orow[0] = row[37] &= orow[0];
                    orow[1] = row[36] &= orow[1];
                    orow[2] = row[35] &= orow[2];
                    orow[3] = row[34] &= orow[3];
                    orow[4] = row[33] &= orow[4];
                    orow[5] = row[32] &= orow[5];
                    orow[6] = row[31] &= orow[6];
                    orow[7] = row[30] &= orow[7];
                    orow[8] = row[29] &= orow[8];
                    orow[9] = row[28] &= orow[9];
                    orow[10] = row[27] &= orow[10];
                    orow[11] = row[26] &= orow[11];
                    orow[12] = row[25] &= orow[12];
                    orow[13] = row[24] &= orow[13];
                    orow[14] = row[23] &= orow[14];
                    orow[15] = row[22] &= orow[15];
                    orow[16] = row[21] &= orow[16];
                    orow[17] = row[20] &= orow[17];
                    orow[18] = row[19] &= orow[18];
                    orow[19] = row[18] &= orow[19];
                    orow[20] = row[17] &= orow[20];
                    orow[21] = row[16] &= orow[21];
                    orow[22] = row[15] &= orow[22];
                    orow[23] = row[14] &= orow[23];
                    orow[24] = row[13] &= orow[24];
                    orow[25] = row[12] &= orow[25];
                    orow[26] = row[11] &= orow[26];
                    orow[27] = row[10] &= orow[27];
                    orow[28] = row[9] &= orow[28];
                    orow[29] = row[8] &= orow[29];
                    orow[30] = row[7] &= orow[30];
                    orow[31] = row[6] &= orow[31];
                    orow[32] = row[5] &= orow[32];
                    orow[33] = row[4] &= orow[33];
                    orow[34] = row[3] &= orow[34];
                    orow[35] = row[2] &= orow[35];
                    orow[36] = row[1] &= orow[36];
                    orow[37] = row[0] &= orow[37];
                    break;
                case 37:
                    orow[0] = row[36] &= orow[0];
                    orow[1] = row[35] &= orow[1];
                    orow[2] = row[34] &= orow[2];
                    orow[3] = row[33] &= orow[3];
                    orow[4] = row[32] &= orow[4];
                    orow[5] = row[31] &= orow[5];
                    orow[6] = row[30] &= orow[6];
                    orow[7] = row[29] &= orow[7];
                    orow[8] = row[28] &= orow[8];
                    orow[9] = row[27] &= orow[9];
                    orow[10] = row[26] &= orow[10];
                    orow[11] = row[25] &= orow[11];
                    orow[12] = row[24] &= orow[12];
                    orow[13] = row[23] &= orow[13];
                    orow[14] = row[22] &= orow[14];
                    orow[15] = row[21] &= orow[15];
                    orow[16] = row[20] &= orow[16];
                    orow[17] = row[19] &= orow[17];
                    orow[18] = row[18] &= orow[18];
                    orow[19] = row[17] &= orow[19];
                    orow[20] = row[16] &= orow[20];
                    orow[21] = row[15] &= orow[21];
                    orow[22] = row[14] &= orow[22];
                    orow[23] = row[13] &= orow[23];
                    orow[24] = row[12] &= orow[24];
                    orow[25] = row[11] &= orow[25];
                    orow[26] = row[10] &= orow[26];
                    orow[27] = row[9] &= orow[27];
                    orow[28] = row[8] &= orow[28];
                    orow[29] = row[7] &= orow[29];
                    orow[30] = row[6] &= orow[30];
                    orow[31] = row[5] &= orow[31];
                    orow[32] = row[4] &= orow[32];
                    orow[33] = row[3] &= orow[33];
                    orow[34] = row[2] &= orow[34];
                    orow[35] = row[1] &= orow[35];
                    orow[36] = row[0] &= orow[36];
                    break;
                case 36:
                    orow[0] = row[35] &= orow[0];
                    orow[1] = row[34] &= orow[1];
                    orow[2] = row[33] &= orow[2];
                    orow[3] = row[32] &= orow[3];
                    orow[4] = row[31] &= orow[4];
                    orow[5] = row[30] &= orow[5];
                    orow[6] = row[29] &= orow[6];
                    orow[7] = row[28] &= orow[7];
                    orow[8] = row[27] &= orow[8];
                    orow[9] = row[26] &= orow[9];
                    orow[10] = row[25] &= orow[10];
                    orow[11] = row[24] &= orow[11];
                    orow[12] = row[23] &= orow[12];
                    orow[13] = row[22] &= orow[13];
                    orow[14] = row[21] &= orow[14];
                    orow[15] = row[20] &= orow[15];
                    orow[16] = row[19] &= orow[16];
                    orow[17] = row[18] &= orow[17];
                    orow[18] = row[17] &= orow[18];
                    orow[19] = row[16] &= orow[19];
                    orow[20] = row[15] &= orow[20];
                    orow[21] = row[14] &= orow[21];
                    orow[22] = row[13] &= orow[22];
                    orow[23] = row[12] &= orow[23];
                    orow[24] = row[11] &= orow[24];
                    orow[25] = row[10] &= orow[25];
                    orow[26] = row[9] &= orow[26];
                    orow[27] = row[8] &= orow[27];
                    orow[28] = row[7] &= orow[28];
                    orow[29] = row[6] &= orow[29];
                    orow[30] = row[5] &= orow[30];
                    orow[31] = row[4] &= orow[31];
                    orow[32] = row[3] &= orow[32];
                    orow[33] = row[2] &= orow[33];
                    orow[34] = row[1] &= orow[34];
                    orow[35] = row[0] &= orow[35];
                    break;
                case 35:
                    orow[0] = row[34] &= orow[0];
                    orow[1] = row[33] &= orow[1];
                    orow[2] = row[32] &= orow[2];
                    orow[3] = row[31] &= orow[3];
                    orow[4] = row[30] &= orow[4];
                    orow[5] = row[29] &= orow[5];
                    orow[6] = row[28] &= orow[6];
                    orow[7] = row[27] &= orow[7];
                    orow[8] = row[26] &= orow[8];
                    orow[9] = row[25] &= orow[9];
                    orow[10] = row[24] &= orow[10];
                    orow[11] = row[23] &= orow[11];
                    orow[12] = row[22] &= orow[12];
                    orow[13] = row[21] &= orow[13];
                    orow[14] = row[20] &= orow[14];
                    orow[15] = row[19] &= orow[15];
                    orow[16] = row[18] &= orow[16];
                    orow[17] = row[17] &= orow[17];
                    orow[18] = row[16] &= orow[18];
                    orow[19] = row[15] &= orow[19];
                    orow[20] = row[14] &= orow[20];
                    orow[21] = row[13] &= orow[21];
                    orow[22] = row[12] &= orow[22];
                    orow[23] = row[11] &= orow[23];
                    orow[24] = row[10] &= orow[24];
                    orow[25] = row[9] &= orow[25];
                    orow[26] = row[8] &= orow[26];
                    orow[27] = row[7] &= orow[27];
                    orow[28] = row[6] &= orow[28];
                    orow[29] = row[5] &= orow[29];
                    orow[30] = row[4] &= orow[30];
                    orow[31] = row[3] &= orow[31];
                    orow[32] = row[2] &= orow[32];
                    orow[33] = row[1] &= orow[33];
                    orow[34] = row[0] &= orow[34];
                    break;
                case 34:
                    orow[0] = row[33] &= orow[0];
                    orow[1] = row[32] &= orow[1];
                    orow[2] = row[31] &= orow[2];
                    orow[3] = row[30] &= orow[3];
                    orow[4] = row[29] &= orow[4];
                    orow[5] = row[28] &= orow[5];
                    orow[6] = row[27] &= orow[6];
                    orow[7] = row[26] &= orow[7];
                    orow[8] = row[25] &= orow[8];
                    orow[9] = row[24] &= orow[9];
                    orow[10] = row[23] &= orow[10];
                    orow[11] = row[22] &= orow[11];
                    orow[12] = row[21] &= orow[12];
                    orow[13] = row[20] &= orow[13];
                    orow[14] = row[19] &= orow[14];
                    orow[15] = row[18] &= orow[15];
                    orow[16] = row[17] &= orow[16];
                    orow[17] = row[16] &= orow[17];
                    orow[18] = row[15] &= orow[18];
                    orow[19] = row[14] &= orow[19];
                    orow[20] = row[13] &= orow[20];
                    orow[21] = row[12] &= orow[21];
                    orow[22] = row[11] &= orow[22];
                    orow[23] = row[10] &= orow[23];
                    orow[24] = row[9] &= orow[24];
                    orow[25] = row[8] &= orow[25];
                    orow[26] = row[7] &= orow[26];
                    orow[27] = row[6] &= orow[27];
                    orow[28] = row[5] &= orow[28];
                    orow[29] = row[4] &= orow[29];
                    orow[30] = row[3] &= orow[30];
                    orow[31] = row[2] &= orow[31];
                    orow[32] = row[1] &= orow[32];
                    orow[33] = row[0] &= orow[33];
                    break;
                case 33:
                    orow[0] = row[32] &= orow[0];
                    orow[1] = row[31] &= orow[1];
                    orow[2] = row[30] &= orow[2];
                    orow[3] = row[29] &= orow[3];
                    orow[4] = row[28] &= orow[4];
                    orow[5] = row[27] &= orow[5];
                    orow[6] = row[26] &= orow[6];
                    orow[7] = row[25] &= orow[7];
                    orow[8] = row[24] &= orow[8];
                    orow[9] = row[23] &= orow[9];
                    orow[10] = row[22] &= orow[10];
                    orow[11] = row[21] &= orow[11];
                    orow[12] = row[20] &= orow[12];
                    orow[13] = row[19] &= orow[13];
                    orow[14] = row[18] &= orow[14];
                    orow[15] = row[17] &= orow[15];
                    orow[16] = row[16] &= orow[16];
                    orow[17] = row[15] &= orow[17];
                    orow[18] = row[14] &= orow[18];
                    orow[19] = row[13] &= orow[19];
                    orow[20] = row[12] &= orow[20];
                    orow[21] = row[11] &= orow[21];
                    orow[22] = row[10] &= orow[22];
                    orow[23] = row[9] &= orow[23];
                    orow[24] = row[8] &= orow[24];
                    orow[25] = row[7] &= orow[25];
                    orow[26] = row[6] &= orow[26];
                    orow[27] = row[5] &= orow[27];
                    orow[28] = row[4] &= orow[28];
                    orow[29] = row[3] &= orow[29];
                    orow[30] = row[2] &= orow[30];
                    orow[31] = row[1] &= orow[31];
                    orow[32] = row[0] &= orow[32];
                    break;
                case 32:
                    orow[0] = row[31] &= orow[0];
                    orow[1] = row[30] &= orow[1];
                    orow[2] = row[29] &= orow[2];
                    orow[3] = row[28] &= orow[3];
                    orow[4] = row[27] &= orow[4];
                    orow[5] = row[26] &= orow[5];
                    orow[6] = row[25] &= orow[6];
                    orow[7] = row[24] &= orow[7];
                    orow[8] = row[23] &= orow[8];
                    orow[9] = row[22] &= orow[9];
                    orow[10] = row[21] &= orow[10];
                    orow[11] = row[20] &= orow[11];
                    orow[12] = row[19] &= orow[12];
                    orow[13] = row[18] &= orow[13];
                    orow[14] = row[17] &= orow[14];
                    orow[15] = row[16] &= orow[15];
                    orow[16] = row[15] &= orow[16];
                    orow[17] = row[14] &= orow[17];
                    orow[18] = row[13] &= orow[18];
                    orow[19] = row[12] &= orow[19];
                    orow[20] = row[11] &= orow[20];
                    orow[21] = row[10] &= orow[21];
                    orow[22] = row[9] &= orow[22];
                    orow[23] = row[8] &= orow[23];
                    orow[24] = row[7] &= orow[24];
                    orow[25] = row[6] &= orow[25];
                    orow[26] = row[5] &= orow[26];
                    orow[27] = row[4] &= orow[27];
                    orow[28] = row[3] &= orow[28];
                    orow[29] = row[2] &= orow[29];
                    orow[30] = row[1] &= orow[30];
                    orow[31] = row[0] &= orow[31];
                    break;
                case 31:
                    orow[0] = row[30] &= orow[0];
                    orow[1] = row[29] &= orow[1];
                    orow[2] = row[28] &= orow[2];
                    orow[3] = row[27] &= orow[3];
                    orow[4] = row[26] &= orow[4];
                    orow[5] = row[25] &= orow[5];
                    orow[6] = row[24] &= orow[6];
                    orow[7] = row[23] &= orow[7];
                    orow[8] = row[22] &= orow[8];
                    orow[9] = row[21] &= orow[9];
                    orow[10] = row[20] &= orow[10];
                    orow[11] = row[19] &= orow[11];
                    orow[12] = row[18] &= orow[12];
                    orow[13] = row[17] &= orow[13];
                    orow[14] = row[16] &= orow[14];
                    orow[15] = row[15] &= orow[15];
                    orow[16] = row[14] &= orow[16];
                    orow[17] = row[13] &= orow[17];
                    orow[18] = row[12] &= orow[18];
                    orow[19] = row[11] &= orow[19];
                    orow[20] = row[10] &= orow[20];
                    orow[21] = row[9] &= orow[21];
                    orow[22] = row[8] &= orow[22];
                    orow[23] = row[7] &= orow[23];
                    orow[24] = row[6] &= orow[24];
                    orow[25] = row[5] &= orow[25];
                    orow[26] = row[4] &= orow[26];
                    orow[27] = row[3] &= orow[27];
                    orow[28] = row[2] &= orow[28];
                    orow[29] = row[1] &= orow[29];
                    orow[30] = row[0] &= orow[30];
                    break;
                case 30:
                    orow[0] = row[29] &= orow[0];
                    orow[1] = row[28] &= orow[1];
                    orow[2] = row[27] &= orow[2];
                    orow[3] = row[26] &= orow[3];
                    orow[4] = row[25] &= orow[4];
                    orow[5] = row[24] &= orow[5];
                    orow[6] = row[23] &= orow[6];
                    orow[7] = row[22] &= orow[7];
                    orow[8] = row[21] &= orow[8];
                    orow[9] = row[20] &= orow[9];
                    orow[10] = row[19] &= orow[10];
                    orow[11] = row[18] &= orow[11];
                    orow[12] = row[17] &= orow[12];
                    orow[13] = row[16] &= orow[13];
                    orow[14] = row[15] &= orow[14];
                    orow[15] = row[14] &= orow[15];
                    orow[16] = row[13] &= orow[16];
                    orow[17] = row[12] &= orow[17];
                    orow[18] = row[11] &= orow[18];
                    orow[19] = row[10] &= orow[19];
                    orow[20] = row[9] &= orow[20];
                    orow[21] = row[8] &= orow[21];
                    orow[22] = row[7] &= orow[22];
                    orow[23] = row[6] &= orow[23];
                    orow[24] = row[5] &= orow[24];
                    orow[25] = row[4] &= orow[25];
                    orow[26] = row[3] &= orow[26];
                    orow[27] = row[2] &= orow[27];
                    orow[28] = row[1] &= orow[28];
                    orow[29] = row[0] &= orow[29];
                    break;
            }
        }
    }
}
