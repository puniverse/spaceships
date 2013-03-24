/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships;

import co.paralleluniverse.db.api.Sync;
import co.paralleluniverse.spacebase.AABB;
import static co.paralleluniverse.spacebase.AABB.X;
import static co.paralleluniverse.spacebase.AABB.Y;
import co.paralleluniverse.spacebase.ElementUpdater;
import co.paralleluniverse.spacebase.MutableAABB;
import co.paralleluniverse.spacebase.SpatialModifyingVisitor;
import co.paralleluniverse.spacebase.SpatialQueries;
import co.paralleluniverse.spacebase.SpatialSetVisitor;
import co.paralleluniverse.spacebase.SpatialToken;
import co.paralleluniverse.spacebase.SpatialVisitor;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author pron
 */
public abstract class Spaceship {
    public static Spaceship create(Spaceships global) {
        return create(global, global.mode);
    }

    public static String description(int mode) {
        return create(null, mode).description();
    }

    private static Spaceship create(Spaceships global, int mode) {
        switch (mode) {
            case 1:
                return new Spaceship(global) {
                    @Override
                    public String description() {
                        return "Join to count neighbors, and then a global update.";
                    }

                    @Override
                    public Sync run(final Spaceships global) throws Exception {
                        resetNeighborCounter();
                        return null;
                    }
                };
            case 2:
                return new Spaceship(global) {
                    @Override
                    public String description() {
                        return "Read set query to process neighbors, and then a global update.";
                    }

                    @Override
                    public Sync run(final Spaceships global) throws Exception {
                        return global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialSetVisitor<Spaceship>() {
                            @Override
                            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                                process(resultReadOnly);
                            }
                        });
                    }
                };
            case 3:
                return new Spaceship(global) {
                    @Override
                    public String description() {
                        return "Read (non-set) query to process neighbors, and an update in done()";
                    }

                    @Override
                    public Sync run(final Spaceships global) throws Exception {
                        ax = 0.0;
                        ay = 0.0;
                        return global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialVisitor<Spaceship>() {
                            @Override
                            public void visit(Spaceship elem, SpatialToken token) {
                                process(elem);
                            }

                            @Override
                            public void done() {
                                global.sb.update(token, new SpatialModifyingVisitor<Spaceship>() {
                                    @Override
                                    public void visit(ElementUpdater<Spaceship> updater) {
                                        resetNeighborCounter();
                                        move(global, global.currentTime());
                                        updater.update(getAABB());
                                    }

                                    @Override
                                    public void done() {
                                    }
                                });
                            }
                        });
                    }
                };
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
                                process(resultReadOnly);

                                global.sb.update(token, new SpatialModifyingVisitor<Spaceship>() {
                                    @Override
                                    public void visit(ElementUpdater<Spaceship> updater) {
                                        resetNeighborCounter();
                                        move(global, global.currentTime());
                                        updater.update(getAABB());
                                    }

                                    @Override
                                    public void done() {
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
                        return global.sb.queryForUpdate(SpatialQueries.range(getAABB(), global.range), SpatialQueries.equals((Spaceship) this, getAABB()), false, new SpatialSetVisitor<Spaceship>() {
//                        return global.sb.queryForUpdate(SpatialQueries.range(global.sb.getElement(token).getBounds(), global.range), SpatialQueries.equals(global.sb.getElement(token)), false, new SpatialSetVisitor<Spaceship>() {
                            @Override
                            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                                process(resultReadOnly);

                                for (final ElementUpdater<Spaceship> updater : resultForUpdate) {
                                    assert updater.elem() == self; // Spaceship.this;
                                    move(global, global.currentTime());
                                    updater.update(getAABB());
                                }
                            }
                        });
                    }
                };
            case 6:
                return new Spaceship(global) {
                    @Override
                    public String description() {
                        return "Read/update set query to process neighbors, and update in visit";
                    }

                    @Override
                    public Sync run(final Spaceships global) throws Exception {
                        return global.sb.queryForUpdate(SpatialQueries.range(getAABB(), global.range), SpatialQueries.equals((Spaceship) this, getAABB()), false, new SpatialSetVisitor<Spaceship>() {
//                        return global.sb.queryForUpdate(SpatialQueries.range(global.sb.getElement(token).getBounds(), global.range), SpatialQueries.equals(global.sb.getElement(token)), false, new SpatialSetVisitor<Spaceship>() {
                            @Override
                            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                                process(resultReadOnly);

                                global.sb.update(token, new SpatialModifyingVisitor<Spaceship>() {
                                    @Override
                                    public void visit(ElementUpdater<Spaceship> updater) {
                                        resetNeighborCounter();
                                        move(global, global.currentTime());
                                        updater.update(getAABB());
                                    }

                                    @Override
                                    public void done() {
                                    }
                                });
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
    private static final double SPEED_LIMIT = 50.0;
    private static final double SPEED_BOUNCE_DAMPING = 0.9;
    private static final double MIN_PROXIMITY = 4;
    private long lastMoved = -1L;
    protected double x;
    protected double y;
    protected double vx;
    protected double vy;
    protected double ax;
    protected double ay;
    private volatile int neighbors;
    protected SpatialToken token;
    private final AtomicInteger neighborCounter = new AtomicInteger();
    private Sync sync;

    public Spaceship(Spaceships global) {
        if (global == null)
            return;

        final RandSpatial random = global.random;

        x = random.randRange(global.bounds.min(X), global.bounds.max(X));
        y = random.randRange(global.bounds.min(Y), global.bounds.max(Y));

        final double direction = random.nextDouble() * 2 * Math.PI;
        final double speed = random.nextGaussian() * global.speedVariance;
        setVelocityDir(direction, speed);
    }

    public abstract Sync run(Spaceships global) throws Exception;

    public abstract String description();

    protected void process(Set<Spaceship> neighbors) {
        final int n = neighbors.size();
        this.neighbors = n;

        ax = 0.0;
        ay = 0.0;

        if (n > 1) {
            for (Spaceship s : neighbors) {
                if (s == this)
                    continue;
                processNeighbor(s);
            }
        }
    }

    protected void process(Spaceship s) {
        if (s == this)
            return;
        incNeighbors();
        processNeighbor(s);
    }

    protected void processNeighbor(Spaceship s) {
        final double dx = s.x - x;
        final double dy = s.y - y;
        final double d = mag(dx, dy);

        if (d > MIN_PROXIMITY) {
            final double udx = dx / d;
            final double udy = dy / d;

            double attraction = ATTRACTION / (d * d);
            double rejection = REJECTION / (d * d * d);

            ax += (attraction - rejection) * udx;
            ay += (attraction - rejection) * udy;

            assert !Double.isNaN(ax + ay);
        }
    }

    public void move(Spaceships global, long currentTime) {
        final long duration = currentTime - lastMoved;
        if (lastMoved > 0 & duration > 0) {
            final AABB bounds = global.bounds;

            vx += ax * duration / TimeUnit.SECONDS.toMillis(1);
            vy += ay * duration / TimeUnit.SECONDS.toMillis(1);

            limitSpeed();

            assert !Double.isNaN(vx + vy);

            x += vx * duration / TimeUnit.SECONDS.toMillis(1);
            y += vy * duration / TimeUnit.SECONDS.toMillis(1);
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
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    public void join() throws InterruptedException {
        if (sync != null)
            sync.join();
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

    public double getVx() {
        return vx;
    }

    public double getVy() {
        return vy;
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

    void incNeighbors() {
        neighborCounter.incrementAndGet();
    }

    void resetNeighborCounter() {
        neighbors = neighborCounter.get();
        neighborCounter.set(0);
    }
}
