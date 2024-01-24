package OldRushBot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import java.util.Random;
import java.util.function.BiConsumer;

public class UnrolledUtils {
    /**
     * Helper function to fill an array with a value.
     * @param arr the array to fill.
     * @param val the value with which the array is filled.
     */
    public static void fill(int[] arr, int val) {
        switch (arr.length) {
            case 60: arr[59] = val;
            case 59: arr[58] = val;
            case 58: arr[57] = val;
            case 57: arr[56] = val;
            case 56: arr[55] = val;
            case 55: arr[54] = val;
            case 54: arr[53] = val;
            case 53: arr[52] = val;
            case 52: arr[51] = val;
            case 51: arr[50] = val;
            case 50: arr[49] = val;
            case 49: arr[48] = val;
            case 48: arr[47] = val;
            case 47: arr[46] = val;
            case 46: arr[45] = val;
            case 45: arr[44] = val;
            case 44: arr[43] = val;
            case 43: arr[42] = val;
            case 42: arr[41] = val;
            case 41: arr[40] = val;
            case 40: arr[39] = val;
            case 39: arr[38] = val;
            case 38: arr[37] = val;
            case 37: arr[36] = val;
            case 36: arr[35] = val;
            case 35: arr[34] = val;
            case 34: arr[33] = val;
            case 33: arr[32] = val;
            case 32: arr[31] = val;
            case 31: arr[30] = val;
            case 30: arr[29] = val;
            case 29: arr[28] = val;
            case 28: arr[27] = val;
            case 27: arr[26] = val;
            case 26: arr[25] = val;
            case 25: arr[24] = val;
            case 24: arr[23] = val;
            case 23: arr[22] = val;
            case 22: arr[21] = val;
            case 21: arr[20] = val;
            case 20: arr[19] = val;
            case 19: arr[18] = val;
            case 18: arr[17] = val;
            case 17: arr[16] = val;
            case 16: arr[15] = val;
            case 15: arr[14] = val;
            case 14: arr[13] = val;
            case 13: arr[12] = val;
            case 12: arr[11] = val;
            case 11: arr[10] = val;
            case 10: arr[9] = val;
            case 9: arr[8] = val;
            case 8: arr[7] = val;
            case 7: arr[6] = val;
            case 6: arr[5] = val;
            case 5: arr[4] = val;
            case 4: arr[3] = val;
            case 3: arr[2] = val;
            case 2: arr[1] = val;
            case 1: arr[0] = val;
        }
    }

    public static void copy(int[] arr1, int[] arr2) {
        switch (arr1.length) {
            case 60: arr1[59] = arr2[59];
            case 59: arr1[58] = arr2[58];
            case 58: arr1[57] = arr2[57];
            case 57: arr1[56] = arr2[56];
            case 56: arr1[55] = arr2[55];
            case 55: arr1[54] = arr2[54];
            case 54: arr1[53] = arr2[53];
            case 53: arr1[52] = arr2[52];
            case 52: arr1[51] = arr2[51];
            case 51: arr1[50] = arr2[50];
            case 50: arr1[49] = arr2[49];
            case 49: arr1[48] = arr2[48];
            case 48: arr1[47] = arr2[47];
            case 47: arr1[46] = arr2[46];
            case 46: arr1[45] = arr2[45];
            case 45: arr1[44] = arr2[44];
            case 44: arr1[43] = arr2[43];
            case 43: arr1[42] = arr2[42];
            case 42: arr1[41] = arr2[41];
            case 41: arr1[40] = arr2[40];
            case 40: arr1[39] = arr2[39];
            case 39: arr1[38] = arr2[38];
            case 38: arr1[37] = arr2[37];
            case 37: arr1[36] = arr2[36];
            case 36: arr1[35] = arr2[35];
            case 35: arr1[34] = arr2[34];
            case 34: arr1[33] = arr2[33];
            case 33: arr1[32] = arr2[32];
            case 32: arr1[31] = arr2[31];
            case 31: arr1[30] = arr2[30];
            case 30: arr1[29] = arr2[29];
            case 29: arr1[28] = arr2[28];
            case 28: arr1[27] = arr2[27];
            case 27: arr1[26] = arr2[26];
            case 26: arr1[25] = arr2[25];
            case 25: arr1[24] = arr2[24];
            case 24: arr1[23] = arr2[23];
            case 23: arr1[22] = arr2[22];
            case 22: arr1[21] = arr2[21];
            case 21: arr1[20] = arr2[20];
            case 20: arr1[19] = arr2[19];
            case 19: arr1[18] = arr2[18];
            case 18: arr1[17] = arr2[17];
            case 17: arr1[16] = arr2[16];
            case 16: arr1[15] = arr2[15];
            case 15: arr1[14] = arr2[14];
            case 14: arr1[13] = arr2[13];
            case 13: arr1[12] = arr2[12];
            case 12: arr1[11] = arr2[11];
            case 11: arr1[10] = arr2[10];
            case 10: arr1[9] = arr2[9];
            case 9: arr1[8] = arr2[8];
            case 8: arr1[7] = arr2[7];
            case 7: arr1[6] = arr2[6];
            case 6: arr1[5] = arr2[5];
            case 5: arr1[4] = arr2[4];
            case 4: arr1[3] = arr2[3];
            case 3: arr1[2] = arr2[2];
            case 2: arr1[1] = arr2[1];
            case 1: arr1[0] = arr2[0];
        }
    }

    public static void clearSharedArray() throws GameActionException {
        RobotController local = V.rc;
        local.writeSharedArray(0, 0);
        local.writeSharedArray(1, 0);
        local.writeSharedArray(2, 0);
        local.writeSharedArray(3, 0);
        local.writeSharedArray(4, 0);
        local.writeSharedArray(5, 0);
        local.writeSharedArray(6, 0);
        local.writeSharedArray(7, 0);
        local.writeSharedArray(8, 0);
        local.writeSharedArray(9, 0);
        local.writeSharedArray(10, 0);
        local.writeSharedArray(11, 0);
        local.writeSharedArray(12, 0);
        local.writeSharedArray(13, 0);
        local.writeSharedArray(14, 0);
        local.writeSharedArray(15, 0);
        local.writeSharedArray(16, 0);
        local.writeSharedArray(17, 0);
        local.writeSharedArray(18, 0);
        local.writeSharedArray(19, 0);
        local.writeSharedArray(20, 0);
        local.writeSharedArray(21, 0);
        local.writeSharedArray(22, 0);
        local.writeSharedArray(23, 0);
        local.writeSharedArray(24, 0);
        local.writeSharedArray(25, 0);
        local.writeSharedArray(26, 0);
        local.writeSharedArray(27, 0);
        local.writeSharedArray(28, 0);
        local.writeSharedArray(29, 0);
        local.writeSharedArray(30, 0);
        local.writeSharedArray(31, 0);
        local.writeSharedArray(32, 0);
        local.writeSharedArray(33, 0);
        local.writeSharedArray(34, 0);
        local.writeSharedArray(35, 0);
        local.writeSharedArray(36, 0);
        local.writeSharedArray(37, 0);
        local.writeSharedArray(38, 0);
        local.writeSharedArray(39, 0);
        local.writeSharedArray(40, 0);
        local.writeSharedArray(41, 0);
        local.writeSharedArray(42, 0);
        local.writeSharedArray(43, 0);
        local.writeSharedArray(44, 0);
        local.writeSharedArray(45, 0);
        local.writeSharedArray(46, 0);
        local.writeSharedArray(47, 0);
        local.writeSharedArray(48, 0);
        local.writeSharedArray(49, 0);
    }
}
