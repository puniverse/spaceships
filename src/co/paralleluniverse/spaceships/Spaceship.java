/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships;

import co.paralleluniverse.spacebase.AABB;
import static co.paralleluniverse.spacebase.AABB.X;
import static co.paralleluniverse.spacebase.AABB.Y;
import co.paralleluniverse.spacebase.ElementUpdater;
import co.paralleluniverse.spacebase.MutableAABB;
import co.paralleluniverse.spacebase.SpatialModifyingVisitor;
import co.paralleluniverse.spacebase.SpatialQueries;
import co.paralleluniverse.spacebase.SpatialSetVisitor;
import co.paralleluniverse.spacebase.SpatialToken;
import co.paralleluniverse.spacebase.Sync;
import co.paralleluniverse.spacebase.UpdateVisitor;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public abstract class Spaceship {
    public static final int MAX_SHOOT_RANGE = 400;
    public static final int TIMES_HITTED_TO_BLOW = 3;

    public long getBlowTime() {
        return blowTime;
    }

    public static Spaceship create(Spaceships global) {
        return create(global, global.mode);
    }

    public static String description(int mode) {
        return create(null, mode).description();
    }

    private static Spaceship create(Spaceships global, int mode) {
        switch (mode) {
            case 4:
                return new Spaceship(global) {
                    @Override
                    public String description() {
                        return "Read set query to process neighbors, and an update in visit()";
                    }

                    @Override
                    public Sync run(final Spaceships global) throws Exception {
                        return global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialSetVisitor<Spaceship>() {
                            @Override
                            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                                process(resultReadOnly, global.currentTime());

                                global.sb.update(token, new UpdateVisitor<Spaceship>() {
                                    @Override
                                    public AABB visit(Spaceship elem) {
                                        move(global, global.currentTime());
                                        return getAABB();
                                    }
                                });
                            }
                        });
                    }
                };
            case 5:
                return new Spaceship(global) {
                    @Override
                    public String description() {
                        return "Read/update set query to process neighbors, and update with updater";
                    }

                    @Override
                    public Sync run(final Spaceships global) throws Exception {
                        final Spaceship self = this;

                        final RandSpatial random = global.random;
                        if (self.blowTime>0 & global.currentTime() - self.blowTime > 500) {
                            global.replaceSpaceship(self,Spaceship.create(global));
                            return null;
                        }
                        if (self.blowTime>0) return null;

                        if (global.currentTime() - self.getTimeShot() > 3000 && random.nextFloat() < 0.1)
                            tryToShoot(self, global);

                        return global.sb.queryForUpdate(SpatialQueries.range(getAABB(), global.range), SpatialQueries.equals(getAABB()), new SpatialSetVisitor<Spaceship>() {
                            @Override
                            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                                process(resultReadOnly, global.currentTime());

                                assert resultForUpdate.size() <= 1;
                                for (final ElementUpdater<Spaceship> updater : resultForUpdate) {
                                    assert updater.elem() == self; // Spaceship.this;

                                    move(global, global.currentTime());
                                    updater.update(getAABB());
                                }
                            }
                        });
                    }
                };
            default:
                return null;
        }
    }
    private static final double ATTRACTION = 2000.0;
    private static final double REJECTION = 8000.0;
    private static final double SPEED_LIMIT = 100.0;
    private static final double SPEED_BOUNCE_DAMPING = 0.9;
    private static final double MIN_PROXIMITY = 4;
    private long lastMoved = -1L;
    private long shootTime = 0;
    private double shootLength = 10f;
    private int timesHitted = 0;
    private long blowTime = 0;

    public double getShootLength() {
        return shootLength;
    }
    private long timeShot = 0;

    public long getTimeShot() {
        return timeShot;
    }

    public void shot(final Spaceships global, Spaceship shooter) {
        this.timesHitted++;
        this.timeShot = global.currentTime();
        if (timesHitted< TIMES_HITTED_TO_BLOW) {
            final double dx = shooter.x - x;
            final double dy = shooter.y - y;
            double d = mag(dx, dy);
            if (d < MIN_PROXIMITY)
                d=MIN_PROXIMITY;
            final double udx = dx / d;
            final double udy = dy / d;

            double hitRecoil = -100;

            reduceExAcc(timeShot);
            exVx += hitRecoil * udx;
            exVy += hitRecoil * udy;
            this.exAccUpdated = timeShot;
            
        } else if (blowTime==0) {
            Sync queryForUpdate = global.sb.queryForUpdate(SpatialQueries.range(getAABB(), global.range*2),
                    new SpatialModifyingVisitor<Spaceship>() {

                @Override
                public void visit(ElementUpdater<Spaceship> updater) {
                    updater.elem().blast(global,Spaceship.this);
                }

                @Override
                public void done() {}
            });
            blowTime = global.currentTime();
        }


    }

    public long getShootTime() {
        return shootTime;
    }
    protected double x;
    protected double y;
    protected double vx;
    protected double vy;
    protected double ax;
    protected double ay;
    protected long exAccUpdated = 0;
    protected double exVx = 0;
    protected double exVy = 0;
    protected double exAx = 0;
    protected double exAy = 0;
    private volatile int neighbors;
    protected SpatialToken token;

//    private final AtomicInteger neighborCounter = new AtomicInteger();
    public Spaceship(Spaceships global) {
        if (global == null)
            return;

        final RandSpatial random = global.random;

        x = random.randRange(global.bounds.min(X), global.bounds.max(X));
        y = random.randRange(global.bounds.min(Y), global.bounds.max(Y));

        final double direction = random.nextDouble() * 2 * Math.PI;
        final double speed = SPEED_LIMIT / 4 + random.nextGaussian() * global.speedVariance;
        setVelocityDir(direction, speed);
    }

    public abstract Sync run(Spaceships global) throws Exception;

    public abstract String description();

    protected void process(Set<Spaceship> neighbors, long currentTime) {
        final int n = neighbors.size();
        this.neighbors = n;

        ax = 0.0;
        ay = 0.0;

        if (n > 1) {
            for (Spaceship s : neighbors) {
                if (s == this)
                    continue;
                processNeighbor(s, currentTime);
            }
        }
    }

    protected void process(Spaceship s, long currentTime) {
        if (s == this)
            return;
        processNeighbor(s, currentTime);
    }

    protected void tryToShoot(final Spaceship self, final Spaceships global) {
        double v = Math.sqrt(Math.pow(self.vx, 2) + Math.pow(self.vy, 2));
        double x2 = self.x + self.vx / v * MAX_SHOOT_RANGE;
        double y2 = self.y + self.vy / v * MAX_SHOOT_RANGE;

        global.sb.queryForUpdate(SpatialQueries.NONE_QUERY, new LineQuery<Spaceship>(x + 1, x2, y + 1, y2), new SpatialSetVisitor<Spaceship>() {
            @Override
            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                ElementUpdater<Spaceship> closeShip = null;
                double minRange2 = Math.pow(MAX_SHOOT_RANGE, 2);
                for (ElementUpdater<Spaceship> eu : resultForUpdate) {
                    double rng2 = Math.pow(eu.elem().x - self.x, 2)
                            + Math.pow(eu.elem().y - self.y, 2);
                    if (rng2 > 100 & rng2 <= minRange2) { //not me and not so close
                        minRange2 = rng2;
                        closeShip = eu;
                    }
                    if (closeShip != null) {

                        self.shootTime = global.currentTime();
                        self.shootLength = Math.sqrt(minRange2);
                        eu.elem().shot(global, self);
                    }
                }
            }
        });
    }

    protected void processNeighbor(Spaceship s, long currentTime) {
        final double dx = s.x - x;
        final double dy = s.y - y;
        double d = mag(dx, dy);
        final double udx = dx / d;
        final double udy = dy / d;

        double attraction = 0.0;
        double rejection = 0.0;

        double mult = 10;

//        mult *= Math.max(-19/2000 * (currentTime-s.timeShot) + 20,1);
        if (d < MIN_PROXIMITY)
            d = MIN_PROXIMITY;
//            attraction = 0 / (d * d);
        rejection = Math.min(mult * REJECTION / (d * d),150);
        
        ax += (attraction - rejection) * udx;
        ay += (attraction - rejection) * udy;

        assert !Double.isNaN(ax + ay);
    }

    public double[] getCurrentPosition(long currentTime) {
        if (blowTime>0) currentTime=blowTime;
        double duration = (double) (currentTime - lastMoved) / TimeUnit.SECONDS.toMillis(1);
        double duration2 = duration * duration;// * Math.signum(duration);
        double pos[] = {x + (vx + exVx) * duration + (exAx + ax) * duration2, y + (vy + exVy) * duration + (exAy + ay) * duration2};
        return pos;
    }

    public double[] getCurrentVelocity(long currentTime) {
        double duration = (double) (currentTime - lastMoved) / TimeUnit.SECONDS.toMillis(1);
        double velocity[] = {vx + (ax) * duration, vy + (ay) * duration};
        return velocity;
    }

    public double getCurrentHeading(long currentTime) {
        double velocity[] = getCurrentVelocity(currentTime);
        return Math.atan2(velocity[0], velocity[1]);
    }

    public void reduceExAcc(long currentTime) {
        double duration = (double) (currentTime - exAccUpdated) / TimeUnit.SECONDS.toMillis(1);;
        if (exAccUpdated > 0 & duration > 0) {
            exVx /= (1 + 8 * duration);
            exVy /= (1 + 8 * duration);
        }
        exAccUpdated = currentTime;

    }

    public void move(Spaceships global, long currentTime) {
        final long duration = currentTime - lastMoved;
        if (lastMoved > 0 & duration > 0) {
            final AABB bounds = global.bounds;
            double pos[] = getCurrentPosition(currentTime);
            x = pos[0];
            y = pos[1];
            double vel[] = getCurrentVelocity(currentTime);
            vx = vel[0];
            vy = vel[1];

            limitSpeed();

            assert !Double.isNaN(vx + vy);

            if (x > bounds.max(X) || x < bounds.min(X)) {
                x = Math.min(x, bounds.max(X));
                x = Math.max(x, bounds.min(X));
                vx = -vx * SPEED_BOUNCE_DAMPING;
            }
            if (y > bounds.max(Y) || y < bounds.min(Y)) {
                y = Math.min(y, bounds.max(Y));
                y = Math.max(y, bounds.min(Y));
                vy = -vy * SPEED_BOUNCE_DAMPING;
            }

            assert !Double.isNaN(x + y);
        }
        this.lastMoved = currentTime;
        reduceExAcc(currentTime);
    }

    public long getLastMoved() {
        return lastMoved;
    }

    private void setVelocityDir(double direction, double speed) {
        vx = speed * cos(direction);
        vy = speed * sin(direction);
        limitSpeed();
    }

    private void limitSpeed() {
        final double speed = mag(vx, vy);
        if (speed > SPEED_LIMIT) {
            vx = vx / speed * SPEED_LIMIT;
            vy = vy / speed * SPEED_LIMIT;
        }
    }

    private double mag(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public AABB getAABB() {
        final MutableAABB aabb = AABB.create(2);
        getAABB(aabb);
        return aabb;
    }

    public void getAABB(MutableAABB aabb) {
        // capture x and y atomically (each)
        final double _x = x;
        final double _y = y;
        aabb.min(X, _x);
        aabb.max(X, _x);
        aabb.min(Y, _y);
        aabb.max(Y, _y);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getNeighbors() {
        return neighbors;
    }

    protected void resetNeighbors() {
        this.neighbors = 0;
    }

    public SpatialToken getToken() {
        return token;
    }

    public void setToken(SpatialToken token) {
        this.token = token;
    }

    private void blast(Spaceships global, Spaceship hitted) {        
        final double dx = hitted.x - x;
        final double dy = hitted.y - y;
        final double d = mag(dx, dy);
        if (d < MIN_PROXIMITY)
            return;
        final double udx = dx / d;
        final double udy = dy / d;

        double hitRecoil = 0.25 * d - 200; 
//                Math.max(-20000/d,-100);

        reduceExAcc(global.currentTime());
        exVx += hitRecoil * udx;
        exVy += hitRecoil * udy;
        this.exAccUpdated = global.currentTime();
    }
}
