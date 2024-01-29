package RushBot;

import battlecode.common.*;
import jdk.nashorn.internal.runtime.GlobalConstants;
import java.util.Random;

public class BugNav {
    static int stackSize = 0;
    static int turnDir = 0;
    static int stackPassIndex = 2;
    static Direction front;
    static Direction back;
    static RobotController rc;
    static Random rng;
    public static void init() {
        rc = V.rc;
        rng = V.rng;
    }
    static Direction turn(Direction d, int turnDir) {
        return turnDir == 0 ? d.rotateLeft() : d.rotateRight();
    }
    static boolean goodDir(Direction d) throws GameActionException {
        boolean moveCooldownDone = rc.getMovementCooldownTurns() == 0;
        MapLocation nextLoc = rc.adjacentLocation(d);
        if(!rc.onTheMap(nextLoc)) return false;
        boolean validFlagCheck = V.flagHome != null && V.rc.hasFlag() && V.round < GameConstants.SETUP_ROUNDS;
        boolean nearWall = false;
        boolean hasCrumbs = rc.onTheMap(nextLoc) && rc.senseMapInfo(nextLoc).getCrumbs() > 0;
        for(MapInfo mi : rc.senseNearbyMapInfos(nextLoc, 2)) {
            if(mi.isWall() || mi.isDam() || (validFlagCheck && !RobotUtils.validFlagPlacement(mi.getMapLocation()))) {
                nearWall = true;
                break;
            }
        }
        boolean fillableWater = nearWall || (nextLoc.x % 2) == (nextLoc.y % 2) || hasCrumbs;
        MapInfo mi = rc.senseMapInfo(nextLoc);
        if(!moveCooldownDone) {
            // if it's water and not a dam, we can fill it
            if(!(validFlagCheck && !RobotUtils.validFlagPlacement(mi.getMapLocation()))
                    && !mi.isWall() && !mi.isDam() && (!mi.isWater() || (fillableWater && !rc.hasFlag()))) {
                return true;
            }
        } else {
            // if we can move there, or if we can fill it
            if((rc.canMove(d) && !(validFlagCheck && !RobotUtils.validFlagPlacement(mi.getMapLocation())))
                    || (rc.canFill(nextLoc) && fillableWater && !rc.hasFlag())) {
                return true;
            }
        }
        RobotInfo nextLocRobot = rc.senseRobotAtLocation(nextLoc);
        // otherwise, if we have the flag and the square we're trying to move to is obstructed by a friend
        if(rc.hasFlag() && nextLocRobot != null && nextLocRobot.getTeam() == rc.getTeam() && V.round > GameConstants.SETUP_ROUNDS) {
            RobotUtils.debug("Passing flag");
            return true;
        }
        return false;
    }
    public static void moveBetter(MapLocation pos) throws GameActionException {
        Direction dirToPos = rc.getLocation().directionTo(pos);
        if(stackSize == 0) {
            front = back = dirToPos;
            stackSize = 1;
        }
        if(turn(front, turnDir) == dirToPos) {
            front = dirToPos;
            stackSize--;
        } else if(turn(front, 1 - turnDir) == dirToPos) {
            front = dirToPos;
            stackSize++;
        }
        if(stackSize != 0 && (dirToPos != front && rc.canMove(dirToPos) || rng.nextInt(32) == 0)) {
            RobotUtils.debug("Stack reset");
            front = back = dirToPos;
            stackSize = 1;
            turnDir = 1 - turnDir;
        }
        if(stackSize >= 3 && goodDir(turn(turn(back, 1 - turnDir), 1 - turnDir))) {
            stackSize -= 2;
            back = turn(turn(back, 1 - turnDir), 1 - turnDir);
        } else if(stackSize >= 2 && goodDir(turn(back, 1 - turnDir))) {
            stackSize--;
            back = turn(back, 1 - turnDir);
        } else {
            RobotUtils.debug("Not turning back");
        }
        boolean triedOtherDir = false;
        while(stackSize < 8) {
            MapLocation nextLoc = rc.adjacentLocation(back);
            if(rc.onTheMap(nextLoc) && !(rc.senseMapInfo(nextLoc).isWall() && RobotUtils.distFromEdge(nextLoc) == 0)) {
                if(goodDir(back)) break;
            } else {
                // reset if hugging wall, try other turn dir
                stackSize = 1;
                back = front;
                if(triedOtherDir) {
                    break;
                }
                turnDir = 1 - turnDir;
                triedOtherDir = true;
            }
            stackSize++;
            back = turn(back, turnDir);
        }
        if(stackSize >= 8) {
            stackSize = 1;
            back = front = rc.getLocation().directionTo(pos);
        }
        RobotUtils.debug(back);
        MapLocation nextLoc = rc.adjacentLocation(back);
        if(!rc.onTheMap(nextLoc)) return;
        RobotInfo nextLocRobot = rc.senseRobotAtLocation(nextLoc);
        if(rc.canFill(nextLoc)) {
            // try adjacent dirs before filling
            if(rc.canMove(turn(back, 1 - turnDir))) rc.move(turn(back, 1 - turnDir));
            else if(rc.canMove(turn(back, turnDir))) rc.move(turn(back, turnDir));
            else rc.fill(nextLoc);
        }
        if(rc.canMove(back)) {
            rc.move(back);
        }
        nextLoc = rc.getLocation().add(back);
        if(rc.onTheMap(nextLoc)) {
            nextLocRobot = rc.senseRobotAtLocation(nextLoc);
            if(rc.canDropFlag(nextLoc) && nextLocRobot != null && nextLocRobot.getTeam() == rc.getTeam()) {
                rc.dropFlag(nextLoc);
                RobotUtils.debug("Passed flag in moveBetter()");
                writeStack();
            }
        }
    }

    public static void writeStack() throws GameActionException {
        // 3 bits for stack size, 1 bit for turn dir, 3 bits for first dir;
        if(stackSize == 0) {
            rc.writeSharedArray(stackPassIndex, 0);
            return;
        }
        for(int i = 0; i < 8; i++) {
            if(front == Direction.values()[i]) {
                rc.writeSharedArray(stackPassIndex, (stackSize << 4) + (turnDir << 3) + i);
                return;
            }
        }
    }
    public static void readStack() throws GameActionException {
        int data = rc.readSharedArray(stackPassIndex);
        if(data == 0) {
            return;
        }
        stackSize = data >> 4;
        turnDir = (data >> 3) & 1;
        front = back = Direction.values()[data & 7];
        for(int i = 0; i < stackSize; i++) {
            back = turn(back, turnDir);
        }
        rc.writeSharedArray(stackPassIndex, 0);
    }
}
