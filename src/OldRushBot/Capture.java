package OldRushBot;

import battlecode.common.*;

public class Capture {
    public static void capture() throws GameActionException {
        FlagInfo[] oppFlags = V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent());
        RobotInfo[] friends = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
        RobotInfo[] enemies = V.rc.senseNearbyRobots(9, V.rc.getTeam().opponent());
        if (oppFlags.length > 0) {
            if (V.rc.hasFlag()) {
                Bfs.moveBfsUtil();
                return;
            }
            RobotUtils.shuffle(oppFlags);
            for (FlagInfo flag: oppFlags) {
                if (!flag.isPickedUp()) {
                    MapLocation flagLoc = flag.getLocation();

                    boolean canPickup = V.rc.canPickupFlag(flagLoc);
                    if (V.spawnBfs == null) {
                        if (V.rc.getLocation().equals(flagLoc)) {
                            if (canPickup) {
                                // the optimal move is not known, so only pickup if we are currently on the flag
                                // not the most efficient flag passing but prevents back and forth
                                V.rc.pickupFlag(flagLoc);
                                BugNav.moveBetter(RobotUtils.closest(V.spawns));
                                System.out.println("No bfs, greedy pickup");
                            }
                            return;
                        }
                        continue;
                    }

                    // first, see if we're in an optimal position to pick it up
                    if (canPickup) {
                        if (V.spawnBfs.dist(V.rc.getLocation()) < V.spawnBfs.dist(flagLoc)) {
                            V.rc.pickupFlag(flagLoc);
                            System.out.println("In an optimal position, picking up");
                            return;
                        }
                    }

                    if (!V.rc.hasFlag()) {
                        // second, check if another friend with HIGHER IDX is in an optimal position to pick it up
                        boolean canFriendPickup = false;
                        for (RobotInfo friend: V.rc.senseNearbyRobots(-1, V.team)) {
                            if (friend.getLocation().isWithinDistanceSquared(flagLoc, 2)) {
                                if (V.spawnBfs.dist(friend.getLocation()) < V.spawnBfs.dist(V.rc.getLocation())) {
                                    if (V.idx[friend.getID() - 10000] > V.selfIdx) {
                                        canFriendPickup = true;
                                        System.out.println("Letting " + friend.getID() + " pickup");
                                        break;
                                    }
                                }
                            }
                        }

                        if (!canFriendPickup) {
                            // then, try move to an optimal position to pick it up
                            RobotUtils.shuffle(V.shuffledDirections);
                            for (int i = 8; --i >= 0;) {
                                Direction dir = V.shuffledDirections[i];
                                if (V.rc.canMove(dir)) {
                                    MapLocation next = V.rc.adjacentLocation(dir);
                                    if (flagLoc.isWithinDistanceSquared(next, GameConstants.INTERACT_RADIUS_SQUARED) && V.spawnBfs.dist(next) < V.spawnBfs.dist(flagLoc)) {
                                        V.rc.move(dir);
                                        System.out.println("Moved to optimal position");
                                        break;
                                    }
                                }
                            }
                            // if unable to move to an optimal position, don't bother
                            // at such short distances, BugNav might produce undesirable behaviours

                            // pickup the flag, at last
                            if (V.rc.canPickupFlag(flagLoc)) {
                                V.rc.pickupFlag(flagLoc);
                                System.out.println("Picked up!");
                            }
                        }
                    }
                }
            }
            V.lastFlag = (V.rc.senseBroadcastFlagLocations().length + oppFlags.length) <= 1 || V.round >= 1750;
            if (V.lastFlag) RobotUtils.debug("LAST FLAG");
            for (FlagInfo flag: V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent())) {
                if (flag.isPickedUp() && V.lastFlag) {
                    RobotUtils.debug("LET'S SWARM");
                    MapLocation loc = flag.getLocation();
                    BugNav.moveBetter(loc);
                    if (V.rc.canSenseLocation(loc) && V.rc.canHeal(loc)) {
                        V.rc.heal(loc);
                    }
                }
            }
        }
    }
}
