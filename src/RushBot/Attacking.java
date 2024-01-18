package RushBot;

import battlecode.common.*;

public class Attacking {
    static void attack() throws GameActionException {
        RobotInfo[] possibleEnemies = V.rc.senseNearbyRobots(4, V.rc.getTeam().opponent());
        // prioritise flag carriers, then sitting ducks, tiebreak by lowest hp
        // todone: implement prioritise sitting ducks
        // Not sure if it was done correctly but should be done
        if (possibleEnemies.length >= 1) { // Check if there are enemies in range
            RobotInfo attackTarget = possibleEnemies[0];
            int currPriority = 0;
            for(RobotInfo enemy : possibleEnemies) {
                boolean flagPriority = enemy.hasFlag;
                boolean stunPriority = false;
                /*
                // nullptr exception due to sittingDucks being uninitialised
                // because it's calculated after first call of attack()
                for (RobotInfo stunned : sittingDucks) {
                    if (stunned == null) {
                        continue;
                    }
                    if (stunned.ID == enemy.ID) {
                        stunPriority = true;
                        break;
                    }
                }*/
                int totalPriority = (flagPriority?2:0)+(stunPriority?1:0);
                // If target is of higher priority than current, retarget
                // If target is of same priority as current, and has less health, retarget
                if (totalPriority == 3) {
                    totalPriority = 2;
                } else if (totalPriority == 2) {
                    totalPriority = 3;
                }
                if(V.rc.canAttack(enemy.getLocation())
                        && (totalPriority > currPriority
                        || (totalPriority == currPriority && enemy.health < attackTarget.health))) {
                    attackTarget = enemy;
                    currPriority = totalPriority;
                }
            }
            if(V.rc.canAttack(attackTarget.getLocation())) {
                V.rc.attack(attackTarget.getLocation());
            }
        }
    }
}
