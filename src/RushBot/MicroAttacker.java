package RushBot;

import battlecode.common.*;

public class MicroAttacker extends Micro {
    private static double[] damage = {150, 157.5, 160.5, 165, 195, 202.5, 240};
    private static final double[] cooldown = {20, 19, 18.6, 18, 16, 13, 8};
    private static double[] dps;

    private boolean severelyHurt;

    MicroAttacker() {
        super();
        dps = new double[7];
        updateDps();
    }

    @Override
    public boolean doMicro() {
        if (Attacking.attack()) {
            Attacking.attack();
        }
        if (!V.rc.isMovementReady()) {
            return false;
        }

        severelyHurt = V.rc.getHealth() <= V.severelyHurt[V.rc.getLevel(SkillType.ATTACK)];
        if (severelyHurt) {
            RobotUtils.debug("Hurt");
        }

        MicroInfo[] microInfos = {
                new MicroInfo(dirs[0]),
                new MicroInfo(dirs[1]),
                new MicroInfo(dirs[2]),
                new MicroInfo(dirs[3]),
                new MicroInfo(dirs[4]),
                new MicroInfo(dirs[5]),
                new MicroInfo(dirs[6]),
                new MicroInfo(dirs[7]),
                new MicroInfo(dirs[8])
        };

        for (RobotInfo ally: V.allies) {
            MapLocation loc = ally.getLocation();
            double allyDps = dps[ally.getAttackLevel()];
            double allyHps = Healing.hps[ally.getHealLevel()];
            microInfos[0].updateAlly(loc, allyDps, allyHps);
            microInfos[1].updateAlly(loc, allyDps, allyHps);
            microInfos[2].updateAlly(loc, allyDps, allyHps);
            microInfos[3].updateAlly(loc, allyDps, allyHps);
            microInfos[4].updateAlly(loc, allyDps, allyHps);
            microInfos[5].updateAlly(loc, allyDps, allyHps);
            microInfos[6].updateAlly(loc, allyDps, allyHps);
            microInfos[7].updateAlly(loc, allyDps, allyHps);
            microInfos[8].updateAlly(loc, allyDps, allyHps);
        }

        for (RobotInfo enemy: V.enemies) {
            MapLocation loc = enemy.getLocation();
            double enemyDps = dps[enemy.getAttackLevel()];
            int enemyHealth = enemy.getHealth();
            microInfos[0].updateEnemy(loc, enemyDps, enemyHealth);
            microInfos[1].updateEnemy(loc, enemyDps, enemyHealth);
            microInfos[2].updateEnemy(loc, enemyDps, enemyHealth);
            microInfos[3].updateEnemy(loc, enemyDps, enemyHealth);
            microInfos[4].updateEnemy(loc, enemyDps, enemyHealth);
            microInfos[5].updateEnemy(loc, enemyDps, enemyHealth);
            microInfos[6].updateEnemy(loc, enemyDps, enemyHealth);
            microInfos[7].updateEnemy(loc, enemyDps, enemyHealth);
            microInfos[8].updateEnemy(loc, enemyDps, enemyHealth);
        }

        MicroInfo best = microInfos[0];
        if (microInfos[1].isBetter(best)) best = microInfos[1];
        if (microInfos[2].isBetter(best)) best = microInfos[2];
        if (microInfos[3].isBetter(best)) best = microInfos[3];
        if (microInfos[4].isBetter(best)) best = microInfos[4];
        if (microInfos[5].isBetter(best)) best = microInfos[5];
        if (microInfos[6].isBetter(best)) best = microInfos[6];
        if (microInfos[7].isBetter(best)) best = microInfos[7];
        if (microInfos[8].isBetter(best)) best = microInfos[8];

        for (MicroInfo info: microInfos) {
            if (V.rc.canMove(info.dir)) {
                int val = 100 + info.safe() * 50;
                V.rc.setIndicatorDot(info.loc, val, val, val);
            }
        }

        if (best.dir == Direction.CENTER || V.rc.canMove(best.dir)) {
            if (V.allies.length + 1 <= V.enemies.length * 5 || !V.rc.isActionReady()) {
                if (best.dir != Direction.CENTER) {
                    Movement.move(best.dir);
                }
            } else {
                try {
                    Building.updateStuns();
                    V.targetCell = Targetting.findTarget();
                    BugNav.moveBetter(V.targetCell);
                } catch (GameActionException e) {
                    System.out.println("GameActionException");
                    e.printStackTrace();
                }
            }
            if (Attacking.attack()) {
                Attacking.attack();
            }
            Healing.heal(true);
        }

        return false;
    }

    private class MicroInfo {
        Direction dir;
        public MapLocation loc;
        MapInfo info;
        boolean canMove;
        double enemyDps = 0;
        double allyHps = 0;
        double enemyPotDps = 0;
        int enemyReach = 0;
        int allyReach = 0;
        int territory = 0;
        int closest = 10000;
        int allyVisibleDps = 0;
        int enemyVisibleDps = 0;
        int minHit = 10000;
        int minHealth = 10000;

        public MicroInfo(Direction dir) {
            this.dir = dir;
            loc = V.rc.adjacentLocation(dir);
            canMove = dir == Direction.CENTER || V.rc.canMove(dir);
            if (canMove) {
                try {
                    info = V.rc.senseMapInfo(loc);
                    if (info.getTeamTerritory() == V.opp) {
                        territory = 2;
                    } else if (info.getTeamTerritory() == Team.NEUTRAL) {
                        territory = 1;
                    }
                } catch (GameActionException e) {
                    System.out.println("GameActionException");
                    e.printStackTrace();
                }
            }
        }

        public void updateEnemy(MapLocation loc, double dps, int health) {
            if (canMove) {
                int dist = this.loc.distanceSquaredTo(loc);
                closest = StrictMath.min(closest, dist);
                if (dist <= 4) {
                    enemyDps += dps;
                    enemyReach++;
                    minHit = StrictMath.min(minHit, (health - 1) / V.rc.getAttackDamage() + 1);
                    minHealth = StrictMath.min(minHealth, health);
                } else if (dist <= 10) {
                    enemyPotDps += dps;
                }
                if (dist <= 20) {
                    enemyVisibleDps += dps;
                }
            }
        }

        public void updateAlly(MapLocation loc, double dps, double hps) {
            if (canMove) {
                int dist = this.loc.distanceSquaredTo(loc);
                if (dist <= 4) {
                    allyHps += hps;
                    allyReach++;
                }
                if (dist <= 20) {
                    allyVisibleDps += dps;
                }
            }
        }

        private int safe() {
            if (!canMove) {
                return -1;
            }
            if (enemyDps > 0) {
                return 0;
            }
            return 1;
        }

        private boolean inRange() {
            return closest <= 4;
        }

        public boolean isBetter(MicroInfo m) {
            if (canMove != m.canMove) {
                return canMove;
            }
            if (severelyHurt) {
                if (inRange() != m.inRange()) {
                    return !inRange();
                }
                if (enemyDps != m.enemyDps) {
                    return enemyDps < m.enemyDps;
                }
                if (allyHps != m.allyHps) {
                    return allyHps > m.allyHps;
                }
                if (enemyPotDps != m.enemyPotDps) {
                    return enemyPotDps < m.enemyPotDps;
                }
                return closest < m.closest;
            }
            if (V.rc.isActionReady()) {
                if (inRange() != m.inRange()) {
                    return inRange();
                }
                if (minHit == 1 && m.minHit > 1) {
                    return true;
                }
                if (minHit > 1 && m.minHit == 1) {
                    return false;
                }
            } else {
                if (inRange() != m.inRange()) {
                    return !inRange();
                }
            }
            if (enemyDps != m.enemyDps) {
                return enemyDps < m.enemyDps;
            }
            if (enemyPotDps != m.enemyPotDps) {
                return enemyPotDps < m.enemyPotDps;
            }
            if (closest != m.closest) {
                return closest < m.closest;
            }
            return allyHps > m.allyHps;
        }
    }

    void updateDamage() {
        damage[0] += 60;
        damage[1] += 60;
        damage[2] += 60;
        damage[3] += 60;
        damage[4] += 60;
        damage[5] += 60;
        damage[6] += 60;
        updateDps();
    }

    void updateDps() {
        dps[0] = damage[0] * 10 / cooldown[0];
        dps[1] = damage[1] * 10 / cooldown[1];
        dps[2] = damage[2] * 10 / cooldown[2];
        dps[3] = damage[3] * 10 / cooldown[3];
        dps[4] = damage[4] * 10 / cooldown[4];
        dps[5] = damage[5] * 10 / cooldown[5];
        dps[6] = damage[6] * 10 / cooldown[6];
    }
}
