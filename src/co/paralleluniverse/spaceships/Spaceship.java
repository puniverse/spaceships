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
import co.paralleluniverse.spacebase.SpatialQueries;
import co.paralleluniverse.spacebase.SpatialSetVisitor;
import co.paralleluniverse.spacebase.SpatialToken;
import co.paralleluniverse.spacebase.Sync;
import co.paralleluniverse.spacebase.UpdateVisitor;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author pron
 */
public abstract class Spaceship {
    public static Spaceship create(Spaceships global) {
        final int mode = global.mode;
        switch (mode) {
            case 1:
                return new Spaceship(global) {
                    @Override
                    public Sync run(final Spaceships global) throws Exception {
                        resetNeighbors();
                        move(global);
                        return global.sb.update(token, getAABB());
                    }
                };
            case 2:
                return new Spaceship(global) {
                    @Override
                    public Sync run(final Spaceships global) throws Exception {
                        return global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialSetVisitor<Spaceship>() {
                            @Override
                            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                                process(resultReadOnly);

                                global.sb.update(token, new UpdateVisitor<Spaceship>() {
                                    @Override
                                    public AABB visit(Spaceship elem) {
                                        move(global);
                                        return getAABB();
                                    }
                                });
                            }
                        });
                    }
                };
            case 3:
                return new Spaceship(global) {
                    @Override
                    public Sync run(final Spaceships global) throws Exception {
                        final Spaceship self = this;
                        return global.sb.queryForUpdate(SpatialQueries.range(getAABB(), global.range), SpatialQueries.equals(global.sb.getElement(token)), new SpatialSetVisitor<Spaceship>() {
                            @Override
                            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                                process(resultReadOnly);

                                final ElementUpdater<Spaceship> updater = resultForUpdate.iterator().next();
                                assert updater.elem() == self; // Spaceship.this;

                                move(global);
                                updater.update(getAABB());
                            }
                        });
                    }
                };
            case 4:
                return new Spaceship(global) {
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
            default:
                return null;
        }
    }
    private static final double ATTRACTION = 200.0;
    private static final double REJECTION = 300.0;
    private static final double SPEED_LIMIT = 10.0;
    
    private double x;
    private double y;
    private double vx;
    private double vy;
    private volatile int neighbors;
    protected SpatialToken token;
    private double dvx;
    private double dvy;
    private final AtomicInteger neighborCounter = new AtomicInteger();

    public Spaceship(Spaceships global) {
        final RandSpatial random = global.random;

        x = random.randRange(global.bounds.min(X), global.bounds.max(X));
        y = random.randRange(global.bounds.min(Y), global.bounds.max(Y));

        final double direction = random.nextDouble() * 2 * Math.PI;
        final double speed = random.nextGaussian() * global.speedVariance;
        setVelocityDir(direction, speed);
    }

    public abstract Sync run(Spaceships global) throws Exception;

    protected void process(Set<Spaceship> neighbors) {
        final int n = neighbors.size();
        this.neighbors = n;

        dvx = 0.0;
        dvy = 0.0;

        if (n > 1) {
            for (Spaceship s : neighbors) {
                if (s == this)
                    continue;
                final double dx = s.x - x;
                final double dy = s.y - y;
                final double d = mag(dx, dy);
                final double udx = dx / d;
                final double udy = dy / d;

                double attraction = ATTRACTION / (d * d);
                double rejection = REJECTION / (d * d * d);

                dvx += (attraction - rejection) * udx;
                dvy += (attraction - rejection) * udx;
            }
        }
    }

    public void move(Spaceships global) {
        final AABB bounds = global.bounds;

        final double alpha = 0.8;
        vx += alpha * dvx;
        vy += alpha * dvy;
        limitSpeed();
        
        if ((x + vx) > bounds.max(X) || (x + vx) < bounds.min(X))
            vx = -vx;
        if ((y + vy) > bounds.max(Y) || (y + vy) < bounds.min(Y))
            vy = -vy;

        x += vx;
        y += vy;
    }

    private void setVelocityDir(double direction, double speed) {
        vx = speed * cos(direction);
        vy = speed * sin(direction);
        limitSpeed();
    }

    private void limitSpeed() {
        final double speed = mag(vx, vy);
        if(speed > SPEED_LIMIT) {
            vx = vx/speed*SPEED_LIMIT;
            vy = vy/speed*SPEED_LIMIT;
        }
    }
    
    private double mag(double x, double y) {
        return Math.sqrt(x*x + y*y);
    }
    
    public AABB getAABB() {
        final MutableAABB aabb = AABB.create(2);
        getAABB(aabb);
        return aabb;
    }

    public void getAABB(MutableAABB aabb) {
        aabb.min(X, x);
        aabb.max(X, x);
        aabb.min(Y, y);
        aabb.max(Y, y);
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

    public SpatialToken getToken() {
        return token;
    }

    public void setToken(SpatialToken token) {
        this.token = token;
    }

    void incNeighbors() {
        neighborCounter.incrementAndGet();
    }

    void resetNeighbors() {
        neighbors = neighborCounter.get();
        neighborCounter.set(0);
    }
}
