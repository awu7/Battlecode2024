package TestBot;

import battlecode.common.*;

public abstract class Micro {
    static final Direction[] dirs = {
            Direction.CENTER,
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST,
    };

    Micro() {}

    abstract boolean doMicro();
}
