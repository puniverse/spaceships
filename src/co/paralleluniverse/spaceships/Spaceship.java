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

    public void run1(final Spaceships global) throws Exception {
        resetNeighbors();
        move(global);
        final Sync sync = global.sb.update(token, getAABB());
        // sync.join();
    }

    public void run2(final Spaceships global) throws Exception {
        final Sync sync = global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialSetVisitor<Spaceship>() {
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
        //sync.join();
    }

    public void run3(final Spaceships global) throws Exception {
        final Sync sync = global.sb.queryForUpdate(SpatialQueries.range(getAABB(), global.range), SpatialQueries.equals(global.sb.getElement(token)), new SpatialSetVisitor<Spaceship>() {
            @Override
            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                process(resultReadOnly);

                final ElementUpdater<Spaceship> updater = resultForUpdate.iterator().next();
                assert updater.elem() == Spaceship.this;
                
                move(global);
                updater.update(getAABB());
            }
        });
        //sync.join();
    }

    public void run4(final Spaceships global) throws Exception {
        final Sync sync = global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialSetVisitor<Spaceship>() {
            @Override
            public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                process(resultReadOnly);
            }
        });
        //sync.join();
    }

    private void process(Set<Spaceship> neighbors) {
        final int n = neighbors.size();
        this.neighbors = n;
        double tx = 0;
        double ty = 0;

        if (n > 1) {
            for (Spaceship s : neighbors) {
                tx += s.x;
                ty += s.y;
            }

            tx /= n;
            double dx = tx - x;
            double dy = ty - y;

            double alpha = 0.2;
//                    vx = (1 - alpha) * vx - alpha * 0.5 * dx;
//                    if (Math.abs(vx) > 10)
//                        vx = Math.signum(vx) * 10;
//
//                    vy = (1 - alpha) * vy - alpha * 0.5 * dy;
//                    if (Math.abs(vy) > 10)
//                        vy = Math.signum(vy) * 10;
        }
    }

    private void setVelocityDir(double direction, double speed) {
        vx = speed * cos(direction);
        vy = speed * sin(direction);
    }

    public void move(Spaceships global) {
        final AABB bounds = global.bounds;

        if ((x + vx) > bounds.max(X) || (x + vx) < bounds.min(X))
            vx = -vx;
        if ((y + vy) > bounds.max(Y) || (y + vy) < bounds.min(Y))
            vy = -vy;

        x += vx;
        y += vy;
    }
}
