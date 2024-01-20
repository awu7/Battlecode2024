package RushBot;

import battlecode.common.*;

public class IDCompression {
    public static void init() {
        V.ids = new int[50];
        V.idx = new int[4097];
    }

    public static void writeID() throws GameActionException {
        int i = -1;
        while (V.rc.readSharedArray(++i) > 0);
        V.rc.writeSharedArray(i, V.id - 10000);
    }

    public static void readIDs() throws GameActionException {
        for (int i = 0; i < 50; ++i) {
            int id = V.rc.readSharedArray(i);
            V.ids[i] = id;
            V.idx[id] = i;
            if (id + 10000 == V.id) {
                V.selfIdx = i;
            }
        }
    }
}
