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
import co.paralleluniverse.spacebase.SpatialSetModifyingVisitor;
import co.paralleluniverse.spacebase.SpatialSetVisitor;
import co.paralleluniverse.spacebase.SpatialToken;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Set;

/**
 *
 * @author pron
 */
public class Spaceship {

    private double x;
    private double y;
    private double vx;
    private double vy;
    private int neighbors;
    private SpatialToken token;

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

    public void run(final Spaceships global) {
        try {
//            global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialSetVisitor<Spaceship>() {
//                @Override
//                public void visit(Set<Spaceship> result) {
//                    neighbors = result.size();
//                    //System.out.println("Seeing " + result.size());
//                }
//
//            }).join();
//            try {
//                move(global);
//                global.sb.update(token, getAABB());
//            } catch (Exception e) {
//                //System.err.println("Exc:" + e.getMessage());
//                e.printStackTrace();
//            } finally {
//                //System.out.println("done");
//            }
//          
            MutableAABB bounds = MutableAABB.create(2);
            getAABB(bounds);
            bounds.min(X, bounds.min(X) - global.range);
            bounds.max(X, bounds.max(X) + global.range);
            bounds.min(Y, bounds.min(Y) - global.range);
            bounds.max(Y, bounds.max(Y) + global.range);
            global.sb.transaction(SpatialQueries.and(SpatialQueries.contained(bounds), SpatialQueries.range(getAABB(), global.range)), new SpatialSetModifyingVisitor<Spaceship>() {

                @Override
                public void visit(Set<ElementUpdater<Spaceship>> result) {
                    neighbors = result.size();
                    move(global);
                    for(ElementUpdater<Spaceship> eu : result) {
                        if(eu.elem() == Spaceship.this)
                            eu.update(getAABB());
                    }
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }).join();
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
