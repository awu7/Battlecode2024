package TestBot;

import battlecode.common.*;

public class MicroAttacker extends Micro {
    private static double[] damage = {150, 157.5, 160.5, 165, 195, 202.5, 240};
    private static final double[] cooldown = {20, 19, 18.6, 18, 16, 13, 8};
    private static double[] dps;
    private static double[] hurtThreshold = {300, 300, 300, 300, 450, 450, 450};

    private boolean severelyHurt;

    MicroAttacker() {
        super();
        dps = new double[7];
        updateDps();
    }

    @Override
    public boolean doMicro() {
        if (!V.rc.isMovementReady()) {
            return false;
        }

        severelyHurt = V.rc.getHealth() <= hurtThreshold[V.rc.getLevel(SkillType.ATTACK)];

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
            microInfos[0].updateAlly(loc, allyDps);
            microInfos[1].updateAlly(loc, allyDps);
            microInfos[2].updateAlly(loc, allyDps);
            microInfos[3].updateAlly(loc, allyDps);
            microInfos[4].updateAlly(loc, allyDps);
            microInfos[5].updateAlly(loc, allyDps);
            microInfos[6].updateAlly(loc, allyDps);
            microInfos[7].updateAlly(loc, allyDps);
            microInfos[8].updateAlly(loc, allyDps);
        }

        for (RobotInfo enemy: V.enemies) {
            MapLocation loc = enemy.getLocation();
            double enemyDps = dps[enemy.getAttackLevel()];
            microInfos[0].updateEnemy(loc, enemyDps);
            microInfos[1].updateEnemy(loc, enemyDps);
            microInfos[2].updateEnemy(loc, enemyDps);
            microInfos[3].updateEnemy(loc, enemyDps);
            microInfos[4].updateEnemy(loc, enemyDps);
            microInfos[5].updateEnemy(loc, enemyDps);
            microInfos[6].updateEnemy(loc, enemyDps);
            microInfos[7].updateEnemy(loc, enemyDps);
            microInfos[8].updateEnemy(loc, enemyDps);
        }

        MicroInfo best = microInfos[8];
        if (microInfos[7].isBetter(best)) best = microInfos[7];
        if (microInfos[6].isBetter(best)) best = microInfos[6];
        if (microInfos[5].isBetter(best)) best = microInfos[5];
        if (microInfos[4].isBetter(best)) best = microInfos[4];
        if (microInfos[3].isBetter(best)) best = microInfos[3];
        if (microInfos[2].isBetter(best)) best = microInfos[2];
        if (microInfos[1].isBetter(best)) best = microInfos[1];
        if (microInfos[0].isBetter(best)) best = microInfos[0];

        if (best.dir == Direction.CENTER) {
            return true;
        }

        if (V.rc.canMove(best.dir)) {
            Movement.move(best.dir);
            return true;
        }
        return false;
    }

    private class MicroInfo {
        Direction dir;
        MapLocation loc;
        MapInfo info;
        boolean canMove;
        double enemyDps = 0;
        double allyDps = 0;
        int enemyReach = 0;
        int allyReach = 0;
        int territory = 0;

        public MicroInfo(Direction dir) {
            this.dir = dir;
            loc = V.rc.adjacentLocation(dir);
            canMove = V.rc.canMove(dir);
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

        public void updateEnemy(MapLocation loc, double dps) {
            if (canMove) {
                if (this.loc.isWithinDistanceSquared(loc, GameConstants.INTERACT_RADIUS_SQUARED)) {
                    enemyDps += dps;
                    enemyReach++;
                }
            }
        }

        public void updateAlly(MapLocation loc, double dps) {
            if (canMove) {
                if (this.loc.isWithinDistanceSquared(loc, GameConstants.INTERACT_RADIUS_SQUARED)) {
                    allyDps += dps;
                    allyReach++;
                }
            }
        }

        public boolean isBetter(MicroInfo m) {
            if (canMove && !m.canMove) {
                return true;
            }
            if (!canMove && m.canMove) {
                return false;
            }
            if (severelyHurt) {
                if (enemyReach < m.enemyReach) {
                    return true;
                }
                if (enemyReach > m.enemyReach) {
                    return false;
                }
                if (allyReach > m.allyReach) {
                    return true;
                }
                if (allyReach < m.allyReach) {
                    return false;
                }
            }
            if (enemyReach == 0 && m.enemyReach > 0) {
                return false;
            }
            if (enemyReach > 0 && m.enemyReach == 0) {
                return true;
            }
            if (enemyDps < m.enemyDps) {
                return true;
            }
            if (enemyDps > m.enemyDps) {
                return false;
            }
            if (allyDps > m.allyDps) {
                return true;
            }
            if (allyDps < m.allyDps) {
                return false;
            }
            if (territory > m.territory) {
                return true;
            }
            if (territory < m.territory) {
                return false;
            }
            return false;
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
