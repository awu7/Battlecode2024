package Merlin;

import battlecode.common.*;

public class Upgrades {
    public static boolean attack, healing, capture;

    /**
     * Helper function attempting to buy global upgrades.
     * Priority:
     * <ol>
     * <li><code>GlobalUpgrade.ATTACK</code></li>
     * <li><code>GlobalUpgrade.HEALING</code></li>
     * <li><code>GlobalUpgrade.CAPTURING</code></li>
     * </ol>
     */
    public static void buyUpgrade() {
        try {
            if (V.rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                V.rc.buyGlobal(GlobalUpgrade.ATTACK);
            }
            if (V.rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                V.rc.buyGlobal(GlobalUpgrade.HEALING);
            }
            if (V.rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                V.rc.buyGlobal(GlobalUpgrade.CAPTURING);
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException");
            e.printStackTrace();
        }
        for (GlobalUpgrade upgrade: V.rc.getGlobalUpgrades(V.team)) {
            switch (upgrade) {
                case ATTACK:
                    if (!attack) {
                        attack = true;
                        if (V.micro instanceof MicroAttacker) {
                            ((MicroAttacker) V.micro).updateDamage();
                        }
                    }
                    break;
                case HEALING:
                    if (!healing) {
                        healing = true;
                        Healing.updateHealing();
                    }
                    break;
                case CAPTURING:
                    if (!capture) {
                        capture = true;
                    }
                    break;
            }
        }
    }
}
