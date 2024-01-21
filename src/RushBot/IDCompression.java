package RushBot;

import battlecode.common.*;

public class IDCompression {
    public static void init() {
        V.ids = new int[50];
        V.idx = new int[4097];
    }

    public static void writeID() throws GameActionException {
        while (V.rc.readSharedArray(++V.selfIdx) > 0);
        V.rc.writeSharedArray(V.selfIdx, V.id - 10000);
    }

    public static void readIDs() throws GameActionException {
        for (int i = 0; i < 50; ++i) {
            int id = V.rc.readSharedArray(i);
            V.ids[i] = id;
            V.idx[id] = i;
        }
    }
}
