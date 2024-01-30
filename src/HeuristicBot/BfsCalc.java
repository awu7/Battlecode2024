package HeuristicBot;

import battlecode.common.*;

import java.util.Locale;
import java.util.function.Consumer;

public class BfsCalc {
    private StringBuilder boardBuilder;
    private final int width, height;
    private final StringBuilder q;
    public int globalMax;

    public boolean done;

    public BfsCalc(int[][] boardArr, MapLocation[] sources, boolean greedy) {
        boardBuilder = new StringBuilder();
        width = boardArr.length;
        height = boardArr[0].length;

        /**
         * 258
         * 147
         * 036
         * will become:
         * \n876\n543\n210
         * reversed:
         * 012\n345\n678\n
         * 012
         * 345
         * 678
         * I.e. rotated 90° clockwise
         */
        StringBuilder local = boardBuilder;
        Consumer<int[]> fill = (int[] row) -> {
            local.append('\n');
            switch (height) {
                case 60: local.append(row[59]);
                case 59: local.append(row[58]);
                case 58: local.append(row[57]);
                case 57: local.append(row[56]);
                case 56: local.append(row[55]);
                case 55: local.append(row[54]);
                case 54: local.append(row[53]);
                case 53: local.append(row[52]);
                case 52: local.append(row[51]);
                case 51: local.append(row[50]);
                case 50: local.append(row[49]);
                case 49: local.append(row[48]);
                case 48: local.append(row[47]);
                case 47: local.append(row[46]);
                case 46: local.append(row[45]);
                case 45: local.append(row[44]);
                case 44: local.append(row[43]);
                case 43: local.append(row[42]);
                case 42: local.append(row[41]);
                case 41: local.append(row[40]);
                case 40: local.append(row[39]);
                case 39: local.append(row[38]);
                case 38: local.append(row[37]);
                case 37: local.append(row[36]);
                case 36: local.append(row[35]);
                case 35: local.append(row[34]);
                case 34: local.append(row[33]);
                case 33: local.append(row[32]);
                case 32: local.append(row[31]);
                case 31: local.append(row[30]);
                case 30: local.append(row[29]);
                    local.append(row[28]);
                    local.append(row[27]);
                    local.append(row[26]);
                    local.append(row[25]);
                    local.append(row[24]);
                    local.append(row[23]);
                    local.append(row[22]);
                    local.append(row[21]);
                    local.append(row[20]);
                    local.append(row[19]);
                    local.append(row[18]);
                    local.append(row[17]);
                    local.append(row[16]);
                    local.append(row[15]);
                    local.append(row[14]);
                    local.append(row[13]);
                    local.append(row[12]);
                    local.append(row[11]);
                    local.append(row[10]);
                    local.append(row[9]);
                    local.append(row[8]);
                    local.append(row[7]);
                    local.append(row[6]);
                    local.append(row[5]);
                    local.append(row[4]);
                    local.append(row[3]);
                    local.append(row[2]);
                    local.append(row[1]);
                    local.append(row[0]);
            }
        };
        switch (width) {
            case 60: fill.accept(boardArr[59]);
            case 59: fill.accept(boardArr[58]);
            case 58: fill.accept(boardArr[57]);
            case 57: fill.accept(boardArr[56]);
            case 56: fill.accept(boardArr[55]);
            case 55: fill.accept(boardArr[54]);
            case 54: fill.accept(boardArr[53]);
            case 53: fill.accept(boardArr[52]);
            case 52: fill.accept(boardArr[51]);
            case 51: fill.accept(boardArr[50]);
            case 50: fill.accept(boardArr[49]);
            case 49: fill.accept(boardArr[48]);
            case 48: fill.accept(boardArr[47]);
            case 47: fill.accept(boardArr[46]);
            case 46: fill.accept(boardArr[45]);
            case 45: fill.accept(boardArr[44]);
            case 44: fill.accept(boardArr[43]);
            case 43: fill.accept(boardArr[42]);
            case 42: fill.accept(boardArr[41]);
            case 41: fill.accept(boardArr[40]);
            case 40: fill.accept(boardArr[39]);
            case 39: fill.accept(boardArr[38]);
            case 38: fill.accept(boardArr[37]);
            case 37: fill.accept(boardArr[36]);
            case 36: fill.accept(boardArr[35]);
            case 35: fill.accept(boardArr[34]);
            case 34: fill.accept(boardArr[33]);
            case 33: fill.accept(boardArr[32]);
            case 32: fill.accept(boardArr[31]);
            case 31: fill.accept(boardArr[30]);
            case 30: fill.accept(boardArr[29]);
                fill.accept(boardArr[28]);
                fill.accept(boardArr[27]);
                fill.accept(boardArr[26]);
                fill.accept(boardArr[25]);
                fill.accept(boardArr[24]);
                fill.accept(boardArr[23]);
                fill.accept(boardArr[22]);
                fill.accept(boardArr[21]);
                fill.accept(boardArr[20]);
                fill.accept(boardArr[19]);
                fill.accept(boardArr[18]);
                fill.accept(boardArr[17]);
                fill.accept(boardArr[16]);
                fill.accept(boardArr[15]);
                fill.accept(boardArr[14]);
                fill.accept(boardArr[13]);
                fill.accept(boardArr[12]);
                fill.accept(boardArr[11]);
                fill.accept(boardArr[10]);
                fill.accept(boardArr[9]);
                fill.accept(boardArr[8]);
                fill.accept(boardArr[7]);
                fill.accept(boardArr[6]);
                fill.accept(boardArr[5]);
                fill.accept(boardArr[4]);
                fill.accept(boardArr[3]);
                fill.accept(boardArr[2]);
                fill.accept(boardArr[1]);
                fill.accept(boardArr[0]);
        }
        local.reverse();
        if (greedy) {
            boardBuilder = new StringBuilder(local.toString().replace('3', '0'));
        }
        q = new StringBuilder();
        for (MapLocation loc: sources) {
            int i = loc.x * V.heightPlus1 + loc.y;
            boardBuilder.setCharAt(i, 'A');
            q.appendCodePoint(i);
        }
        done = false;
//        System.out.println('\n' + local.toString().replace('0', ' '));
    }

    public boolean compute(int minLeft) {
        StringBuilder localBb = boardBuilder, localQ = q;
        int lastLoc = q.charAt(0);
        int wm1 = V.widthMinus1, h = V.height, hp1 = V.heightPlus1, hp2 = V.heightPlus2, hm1 = V.heightMinus1;
        while (q.length() > 0 && Clock.getBytecodesLeft() > minLeft) {
            int i = localQ.charAt(0), j;
            char c = localBb.charAt(i);
            c++;
            int x = i / hp1, y = i % hp1;
            localQ.deleteCharAt(0);
            if (y > 0) {
                j = i - 1;
                if (localBb.charAt(j) == '0') {
                    localBb.setCharAt(j, c);
                    localQ.appendCodePoint(j);
                }
            }
            if (y < hm1) {
                j = i + 1;
                if (localBb.charAt(j) == '0') {
                    localBb.setCharAt(j, c);
                    localQ.appendCodePoint(j);
                }
            }
            if (x > 0) {
                j = i - hp1;
                if (localBb.charAt(j) == '0') {
                    localBb.setCharAt(j, c);
                    localQ.appendCodePoint(j);
                }
            }
            if (x < wm1) {
                j = i + hp1;
                if (localBb.charAt(j) == '0') {
                    localBb.setCharAt(j, c);
                    localQ.appendCodePoint(j);
                }
            }
            if (x > 0 && y > 0) {
                j = i - hp2;
                if (localBb.charAt(j) == '0') {
                    localBb.setCharAt(j, c);
                    localQ.appendCodePoint(j);
                }
            }
            if (x > 0 && y < hm1) {
                j = i - h;
                if (localBb.charAt(j) == '0') {
                    localBb.setCharAt(j, c);
                    localQ.appendCodePoint(j);
                }
            }
            if (x < wm1 && y > 0) {
                j = i + h;
                if (localBb.charAt(j) == '0') {
                    localBb.setCharAt(j, c);
                    localQ.appendCodePoint(j);
                }
            }
            if (x < wm1 && y < hm1) {
                j = i + hp2;
                if (localBb.charAt(j) == '0') {
                    localBb.setCharAt(j, c);
                    localQ.appendCodePoint(j);
                }
            }
        }
        if (q.length() == 0 && !done) {
            done = true;
//            System.out.println(boardBuilder.length());
//            System.out.println('\n' + boardBuilder.toString().replace('0', ' '));
            globalMax = boardBuilder.charAt(lastLoc) - 'A';
            while (boardBuilder.indexOf(String.valueOf((char) ('A' + globalMax + 1))) != -1) {
                globalMax++;
            }
        }
        return q.length() == 0;
    }

    public int dist(MapLocation loc) {
        if (!V.rc.onTheMap(loc)) {
            return 10000;
        }
        int d = boardBuilder.charAt(loc.x * V.heightPlus1 + loc.y) - 'A';
        return d < 0 ? 10000 : d;
    }
}
