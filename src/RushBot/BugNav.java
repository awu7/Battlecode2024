package RushBot;

import battlecode.common.*;

public class BugNav {
    public static void moveBetter(MapLocation pos) throws GameActionException {
        if(V.stackSize != 0 && (!V.rc.getLocation().directionTo(pos).equals(V.stack[0]) && V.rc.canMove(V.rc.getLocation().directionTo(pos)) || V.rng.nextInt(32) == 0)) {
            RobotUtils.debug("Stack reset");
            V.stackSize = 0;
        }
        if(V.stackSize == 0) {
            V.stack[V.stackSize++] = V.rc.getLocation().directionTo(pos);
        }
        if(V.stackSize == 1) {
            V.turnDir = V.rng.nextInt(2);
        }
        if(V.stackSize >= 2 && V.rc.canMove(V.stack[V.stackSize - 2])) {
            V.stackSize--;
        }
        boolean moveCooldownDone = V.rc.getMovementCooldownTurns() == 0;
        MapLocation nextLoc;
        RobotInfo nextLocRobot;
        boolean triedOtherDir = false;
        boolean hasFlag = V.rc.hasFlag() || V.rc.canPickupFlag(V.rc.getLocation());
        boolean fillableWater;
        while(V.stackSize < 8) {
            nextLoc = V.rc.getLocation().add(V.stack[V.stackSize - 1]);
            boolean allowFillNext = (nextLoc.x % 2) == (nextLoc.y % 2);
            boolean nearWall = false;
            boolean hasCrumbs = V.rc.onTheMap(nextLoc) && V.rc.senseMapInfo(nextLoc).getCrumbs() > 0;
            for (MapInfo mi : V.rc.senseNearbyMapInfos(nextLoc, 2)) {
                if (mi.isWall() || mi.isDam()) {
                    nearWall = true;
                    break;
                }
            }
            fillableWater = (nearWall || allowFillNext || hasCrumbs) && V.rc.canFill(nextLoc);
            if(V.rc.onTheMap(nextLoc)) {
                if(!moveCooldownDone) {
                    // if it's not a wall, and if there's water we can fill it
                    if (!V.rc.senseMapInfo(nextLoc).isWall() && (fillableWater || !hasFlag)) {
                        break;
                    }
                } else {
                    // if we can move there, or if we can fill it
                    if (V.rc.canMove(V.stack[V.stackSize - 1]) || (fillableWater && !hasFlag)) {
                        break;
                    }
                }
                nextLocRobot = V.rc.senseRobotAtLocation(nextLoc);
                // otherwise, if we have the flag and the square we're trying to move to is obstructed by a friend
                if (hasFlag && nextLocRobot != null && nextLocRobot.getTeam() == V.rc.getTeam()) {
                    RobotUtils.debug("Passing flag");
                    break;
                }
            } else {
                // reset if hugging wall, try other turn dir
                V.stackSize = 1;
                if(triedOtherDir) {
                    break;
                }
                V.turnDir = 1 - V.turnDir;
                triedOtherDir = true;
            }
            V.stack[V.stackSize] = V.turnDir == 0 ? V.stack[V.stackSize - 1].rotateLeft() : V.stack[V.stackSize - 1].rotateRight();
            V.stackSize++;
        }
        if (V.stackSize >= 8) {
            V.stackSize = 1;
        }
        Direction dir = V.stack[V.stackSize - 1];
        nextLoc = V.rc.getLocation().add(dir);
        boolean allowFillNext = (nextLoc.x % 2) == (nextLoc.y % 2);
        boolean nearWall = false;
        boolean hasCrumbs = V.rc.senseNearbyCrumbs(0).length > 0;
        for (MapInfo mi : V.rc.senseNearbyMapInfos(nextLoc, 2)) {
            if (mi.isWall() || mi.isDam() || hasCrumbs) {
                nearWall = true;
                break;
            }
        }
        if (V.rc.canFill(nextLoc) && !hasFlag) {
            V.rc.fill(nextLoc);
        }
        if(V.rc.canMove(dir)) {
            V.rc.move(dir);
        }
        nextLoc = V.rc.getLocation().add(dir);
        if(V.rc.onTheMap(nextLoc)) {
            nextLocRobot = V.rc.senseRobotAtLocation(nextLoc);
            if(V.rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.getTeam() == V.rc.getTeam()) {
                V.rc.dropFlag(nextLoc);
                RobotUtils.debug("Passed flag in moveBetter()");
                writeStack();
            }
        }
    }

    public static void writeStack() throws GameActionException {
        // 3 bits for stack size, 1 bit for turn dir, 3 bits for first dir;
        if(V.stackSize == 0) {
            V.rc.writeSharedArray(V.stackPassIndex, 0);
            return;
        }
        for(int i = 0; i < 8; i++) {
            if(V.stack[0] == Direction.values()[i]) {
                V.rc.writeSharedArray(V.stackPassIndex, (V.stackSize << 4) + (V.turnDir << 3) + i);
                return;
            }
        }
    }
    public static void readStack() throws GameActionException {
        int data = V.rc.readSharedArray(V.stackPassIndex);
        if(data == 0) {
            return;
        }
        V.stackSize = data >> 4;
        V.turnDir = (data >> 3) & 1;
        V.stack[0] = Direction.values()[data & 7];
        for(int i = 1; i < V.stackSize; i++) {
            V.stack[i] = V.turnDir == 0 ? V.stack[i - 1].rotateLeft() : V.stack[i - 1].rotateRight();
        }
        V.rc.writeSharedArray(V.stackPassIndex, 0);
    }
}
