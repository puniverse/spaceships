/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships;

import co.paralleluniverse.spacebase.AABB;
import static co.paralleluniverse.spacebase.AABB.X;
import static co.paralleluniverse.spacebase.AABB.Y;
import co.paralleluniverse.spacebase.MutableAABB;
import co.paralleluniverse.spacebase.SpatialQueries;
import co.paralleluniverse.spacebase.SpatialSetVisitor;
import co.paralleluniverse.spacebase.SpatialToken;
import co.paralleluniverse.spacebase.SpatialVisitor;
import co.paralleluniverse.spacebase.Sync;
import co.paralleluniverse.spacebase.store.memory.sync.BlockableRecursiveAction;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author pron
 */
public class Spaceship {
    private double x;
    private double y;
    private double vx;
    private double vy;
    private volatile int neighbors;
    private SpatialToken token;
    private final AtomicInteger neighborCounter = new AtomicInteger();

    public Spaceship(Spaceships global) {
        final RandSpatial random = global.random;
        x = random.randRange(global.bounds.min(X), global.bounds.max(X));
        y = random.randRange(global.bounds.min(Y), global.bounds.max(Y));

        final double direction = random.nextDouble() * 2 * Math.PI;
        final double speed = random.nextGaussian() * global.speedVariance;
        setVelocityDir(direction, speed);
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

    public void run(final Spaceships global) throws InterruptedException {
        switch (global.mode) {
            case 1:
                run1(global);
                break;
            case 2:
                run2(global);
                break;
            case 3:
                run3(global);
                break;
        }
    }

    public void run1(final Spaceships global) throws InterruptedException {
        resetNeighbors();
        move(global);
        final Sync sync = global.sb.update(token, getAABB());
        // sync.join();
    }

    public void run2(final Spaceships global) throws InterruptedException {
        final Sync sync = global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialSetVisitor<Spaceship>() {
            @Override
            public void visit(Set<Spaceship> result) {
                final int n = result.size();
                neighbors = n;
                double tx = 0;
                double ty = 0;

                if (n > 1) {
                    for (Spaceship s : result) {
                        tx += s.x;
                        ty += s.y;
                    }

                    tx /= n;
                    double dx = tx - x;
                    double dy = ty - y;

                    double alpha = 0.2;
                    vx = (1 - alpha) * vx - alpha * 0.5 * dx;
                    if (Math.abs(vx) > 10)
                        vx = Math.signum(vx) * 10;

                    vy = (1 - alpha) * vy - alpha * 0.5 * dy;
                    if (Math.abs(vy) > 10)
                        vy = Math.signum(vy) * 10;
                }
                //System.out.println("Seeing " + result.size());
                new BlockableRecursiveAction() {
                    @Override
                    protected void compute() {
                        move(global);
                        global.sb.update(token, getAABB());
                    }
                }.fork();
            }
        });
        sync.join();

//            MutableAABB bounds = MutableAABB.create(2);
//            getAABB(bounds);
//            bounds.min(X, bounds.min(X) - global.range);
//            bounds.max(X, bounds.max(X) + global.range);
//            bounds.min(Y, bounds.min(Y) - global.range);
//            bounds.max(Y, bounds.max(Y) + global.range);
//            global.sb.transaction(SpatialQueries.and(SpatialQueries.contained(bounds), SpatialQueries.range(getAABB(), global.range)), new SpatialSetModifyingVisitor<Spaceship>() {
//
//                @Override
//                public void visit(Set<ElementUpdater<Spaceship>> result) {
//                    neighbors = result.size();
//                    move(global);
//                    for(ElementUpdater<Spaceship> eu : result) {
//                        if(eu.elem() == Spaceship.this)
//                            eu.update(getAABB());
//                    }
//                }
//            }).join();
    }

    public void run3(final Spaceships global) {
        try {
            global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialVisitor<Spaceship>() {
                @Override
                public void visit(Spaceship result, SpatialToken token) {
                    incNeighbors();
                }

                @Override
                public void done() {
                    resetNeighbors();
                    new BlockableRecursiveAction() {
                        @Override
                        protected void compute() {
                            move(global);
                            global.sb.update(token, getAABB());
                        }
                    }.fork();
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setVelocityDir(double direction, double speed) {
        vx = speed * cos(direction);
        vy = speed * sin(direction);
    }

    private void move(Spaceships global) {
        final AABB bounds = global.bounds;

        if ((x + vx) > bounds.max(X) || (x + vx) < bounds.min(X))
            vx = -vx;
        if ((y + vy) > bounds.max(Y) || (y + vy) < bounds.min(Y))
            vy = -vy;

        x += vx;
        y += vy;
    }
}
