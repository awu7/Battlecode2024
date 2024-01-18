package OldRushBot;

import battlecode.common.MapLocation;

public class ActiveStun {
    public MapLocation location = null;
    public int roundsLeft;

    public ActiveStun(MapLocation locIn) {
        location = locIn;
        roundsLeft = 5;
    }

    /**
     * Returns false if this stun is ineffective after updating,
     * otherwise true
     */
    public boolean updateRound() {
        roundsLeft--;
        if (roundsLeft <= 0) {
            return false;
        }
        return true;
    }
}
