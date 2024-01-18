package RushBot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Movement {
    public static void AllMovements() throws GameActionException {
        FlagInfo[] oppFlags = V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent());
        RobotInfo[] friends = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
        RobotInfo[] enemies = V.rc.senseNearbyRobots(9, V.rc.getTeam().opponent());
        MapLocation[] enemyLocs = new MapLocation[enemies.length];
        for (int i = 0; i < enemies.length; ++i) enemyLocs[i] = enemies[i].getLocation();
        // if (oppFlags.length > 0) {
        //     if (V.rc.hasFlag()) {
        //         if (enemies.length > 0 && false) {
        //             V.rc.move(V.rc.getLocation().directionTo(closest(enemyLocs)).opposite());
        //         } else {
        //             moveBfs(BfsTarget.SPAWN);
        //         }
        //         continue;
        //     }
        //     UnrolledUtils.shuffle(oppFlags, rng);
        //     for (FlagInfo flag: oppFlags) {
        //         if (!flag.isPickedUp()) {
        //             MapLocation loc = flag.getLocation();
        //             MapLocation[] next = {};
        //             if (spawnBfs != null) {
        //                 int i = spawnBfs[loc.x][loc.y];
        //                 if (i > 0) {
        //                     Direction opt = directions[i - 1];
        //                     next = new MapLocation[]{loc.add(opt), loc.add(opt.rotateLeft()), loc.add(opt.rotateRight())};
        //                 }
        //             }

        //             // first, see if we're in an optimal position to pick it up
        //             boolean canPickup = V.rc.canPickupFlag(loc);
        //             if (canPickup) {
        //                 for (MapLocation nextLoc : next) {
        //                     if (V.rc.getLocation().equals(nextLoc)) {
        //                         V.rc.pickupFlag(loc);
        //                         break;
        //                     }
        //                 }
        //             }

        //             if (!V.rc.hasFlag()) {
        //                 // second, check if another friend with HIGHER ID is in an optimal position to pick it up
        //                 boolean canFriendPickup = true;
        //                 for (MapLocation nextLoc : next) {
        //                     if (V.rc.onTheMap(nextLoc) && V.rc.canSenseLocation(nextLoc)) {
        //                         RobotInfo friend = V.rc.senseRobotAtLocation(nextLoc);
        //                         if (friend != null && friend.getTeam() == V.rc.getTeam() && idx[V.rc.getID() - 9999] > selfIdx) {
        //                             canFriendPickup = false;
        //                             break;
        //                         }
        //                     }
        //                 }

        //                 if (!canFriendPickup) {
        //                     // then, try move to an optimal position to pick it up
        //                     for (MapLocation nextLoc: next) {
        //                         if (V.rc.onTheMap(nextLoc) && V.rc.canSenseLocation(nextLoc)) {
        //                             Direction dir = V.rc.getLocation().directionTo(nextLoc);
        //                             if (V.rc.adjacentLocation(dir).equals(nextLoc) && V.rc.canMove(dir)) {
        //                                 V.rc.move(dir);
        //                                 break;
        //                             }
        //                         }
        //                     }
        //                     // if unable to move to an optimal position, don't bother
        //                     // at such short distances, BugNav might produce unexpected behaviours

        //                     // pickup the flag, at last
        //                     V.rc.pickupFlag(loc);
        //                 }
        //             }
        //         }
        //     }
        //     lastFlag = (V.rc.senseBroadcastFlagLocations().length + oppFlags.length) <= 1 || round >= 1750;
        //     if (lastFlag) RobotUtils.debug("LAST FLAG");
        //     for (FlagInfo flag: V.rc.senseNearbyFlags(-1, V.rc.getTeam().opponent())) {
        //         if (flag.isPickedUp() && lastFlag) {
        //             RobotUtils.debug("LET'S SWARM");
        //             MapLocation loc = flag.getLocation();
        //             moveBetter(loc);
        //             if (V.rc.canSenseLocation(loc) && V.rc.canHeal(loc)) {
        //                 V.rc.heal(loc);
        //             }
        //         }
        //     }
        // }
        V.nearbyAllies = V.rc.senseNearbyRobots(-1, V.rc.getTeam());
        Capture.pickupFlag(true);
        // Find all triggered stun traps;
        Building.updateStuns();
        if(V.isBuilder) {
            Building.buildTraps();
            Building.farmBuildXp(4);
            Building.farmBuildXp(4);
            Building.farmBuildXp(4);
            Building.farmBuildXp(4);
        }
        if(V.rc.senseNearbyFlags(0).length == 0) {
            Healing.healFlagBearer();
            Attacking.attack();
        }
        V.targetCell = Targetting.findTarget();
        // Determine whether to move or not
        int nearbyHP = V.rc.getHealth();
        for (RobotInfo ally : V.nearbyAllies) {
            nearbyHP += ally.health;
        }
        int threshold = StrictMath.min(V.nearbyAllies.length * 75, 751) * (V.nearbyAllies.length + 1);
        int enemyHP = 0;
        RobotInfo[] rawEnemies = V.rc.senseNearbyRobots(-1, V.rc.getTeam().opponent());
        List<RobotInfo> listEnemies = new ArrayList<RobotInfo>();
        // sittingDucks contains all stunned enemies
        List<RobotInfo> listSittingDucks = new ArrayList<RobotInfo>();
        for(RobotInfo enemy : rawEnemies) {
            boolean skip = false;
            for (ActiveStun stun : V.activeStuns) {
                if (enemy.getLocation().distanceSquaredTo(stun.location) <= 13) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                listSittingDucks.add(enemy);
                V.rc.setIndicatorDot(enemy.getLocation(), 255, 255, 0);
                continue;
            }
            enemyHP += enemy.health;
            listEnemies.add(enemy);
        }
        enemies = listEnemies.toArray(new RobotInfo[0]);
        V.sittingDucks = listSittingDucks.toArray(new RobotInfo[0]);
        // Movement
        {
            if (V.sittingDucks.length > 0 && !V.rc.hasFlag() && !V.isBuilder) {
                int minDistSquared = 100;
                MapLocation[] choices = new MapLocation[V.sittingDucks.length];
                int n = 0;
                for (RobotInfo enemy : V.sittingDucks) {
                    int distToEnemy = enemy.getLocation().distanceSquaredTo(V.rc.getLocation());
                    if (distToEnemy < minDistSquared) {
                        minDistSquared = distToEnemy;
                        n = 0;
                    }
                    if (distToEnemy == minDistSquared) {
                        choices[n++] = enemy.getLocation();
                    }
                }
                MapLocation bestChoice = choices[0];
                if (V.targetCell != null) {
                    int shortestDist = -1;
                    for (int i = 0; i < n; ++i) {
                        MapLocation choice = choices[i];
                        int distToTarget = choice.distanceSquaredTo(V.targetCell);
                        if (shortestDist == -1 || distToTarget < shortestDist) {
                            shortestDist = distToTarget;
                            bestChoice = choice;
                        }
                    }
                }
                V.rc.setIndicatorLine(V.rc.getLocation(), bestChoice, 255, 0, 0);
                //System.out.println("Moving towards a sitting duck");
                BugNav.moveBetter(bestChoice);
            } else if (enemyHP * 5 > nearbyHP * 2
                    && !V.rc.hasFlag()
                    && V.rc.senseNearbyFlags(9, V.rc.getTeam().opponent()).length == 0
                    && V.rc.senseNearbyFlags(4, V.rc.getTeam()).length == 0) {
                // Begin Kiting
                int currTargeted = 0;
                Direction[] choices = new Direction[8];
                MapLocation rcLocation = V.rc.getLocation();
                boolean returnToCombat = V.rc.isActionReady() && V.rc.getHealth() > 750; // Only return if currently safe
                for (RobotInfo enemy : enemies) {
                    if (enemy.getLocation().isWithinDistanceSquared(rcLocation, 4)) {
                        currTargeted++;
                        returnToCombat = false;
                    }
                }
                int minTargeted = currTargeted;
                if (returnToCombat) {
                    minTargeted = 1000; // Reset min
                }
                int n = 0; // size of choices;
                RobotUtils.shuffle(V.shuffledDirections);
                for (Direction dir : V.shuffledDirections) {
                    if (V.rc.canMove(dir)) {
                        int count = 0;
                        for (RobotInfo enemy : enemies) {
                            if (enemy.getLocation().isWithinDistanceSquared(rcLocation.add(dir), 4)) {
                                count++;
                            }
                        }
                        if (!(returnToCombat && count == 0)) {
                            if (count < minTargeted) {
                                minTargeted = count;
                                n = 0;
                            }
                            if (count == minTargeted) {
                                choices[n++] = dir;
                            }
                        }
                    }
                }
                if (n > 0) {
                    // Choose the best choice
                    // Choices are either the safest, or safest where we can attack
                    MapLocation finaltargetCell = V.targetCell;
                    if (V.rc.getHealth() < 900 && enemies.length > 0) {
                        RobotUtils.debug("RETREAT");
                        RobotInfo nearestEnemy = enemies[0];
                        int nearestEnemyDistSquared = 1000;
                        for (RobotInfo enemy : enemies) {
                            int currDistSquared = enemy.getLocation().distanceSquaredTo(V.rc.getLocation());
                            if (currDistSquared < nearestEnemyDistSquared) {
                                nearestEnemyDistSquared = currDistSquared;
                                nearestEnemy = enemy;
                            }
                        }
                        final RobotInfo actualNearestEnemy = nearestEnemy;
                        Arrays.sort(choices, 0, n, (a, b) -> {
                            return V.rc.getLocation().add(b).distanceSquaredTo(actualNearestEnemy.getLocation()) - V.rc.getLocation().add(a).distanceSquaredTo(actualNearestEnemy.getLocation());
                        });
                    } else {
                        Arrays.sort(choices, 0, n, (a, b) -> {
                            return V.rc.getLocation().add(a).distanceSquaredTo(finaltargetCell) - V.rc.getLocation().add(b).distanceSquaredTo(finaltargetCell);
                        });
                    }
                    if (V.rc.canMove(choices[0])) V.rc.move(choices[0]);
                } else {
                    // There are no choices
                    RobotUtils.shuffle(V.shuffledDirections);
                    int minAdj = V.rc.senseNearbyRobots(2, V.rc.getTeam()).length;
                    Direction choice = Direction.NORTH;
                    boolean overridden = false;
                    for (Direction dir : V.shuffledDirections) {
                        if (V.rc.canMove(dir)) {
                            int adjCount = V.rc.senseNearbyRobots(-1, V.rc.getTeam()).length;
                            if (adjCount < minAdj) {
                                choice = dir;
                                minAdj = adjCount;
                                overridden = true;
                            }
                        }
                    }
                    if (overridden) {
                        V.rc.move(choice);
                    } else if (returnToCombat) {
                        // Attempt to move towards enemies
                        if (V.sittingDucks.length > 0) {
                            V.rc.setIndicatorLine(V.rc.getLocation(), V.sittingDucks[0].getLocation(), 255, 0, 0);
                            //System.out.println("Moving towards a sitting duck");
                            BugNav.moveBetter(V.sittingDucks[0].getLocation());
                        } else {
                            BugNav.moveBetter(enemies[0].getLocation());
                        }
                    }
                }
            } else if (nearbyHP >= threshold || V.rc.senseNearbyFlags(13, V.rc.getTeam().opponent()).length == 0) {
                BugNav.moveBetter(V.targetCell);
                RobotUtils.debug(V.targetCell);
                RobotUtils.debug("Nope, not kiting");
            }
        }
    }
}
