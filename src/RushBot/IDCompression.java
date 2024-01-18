package RushBot;

import battlecode.common.*;

public class IDCompression {
    public static void writeID() throws GameActionException {
        int i = 0;
        while (V.rc.readSharedArray(i) > 0) {
            i++;
        }
        V.rc.writeSharedArray(i, V.rc.getID() - 9999);
    }

    public static void readIDs() throws GameActionException {
        for (int i = 0; i < 50; ++i) {
            int id = V.rc.readSharedArray(i);
            V.ids[i] = id;
            V.idx[id] = i;
            if (id + 9999 == V.rc.getID()) {
                V.selfIdx = i;
            }
        }
    }
}
