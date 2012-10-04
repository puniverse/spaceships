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

    public SpatialToken getToken() {
        return token;
    }

    public void setToken(SpatialToken token) {
        this.token = token;
    }

    public void run(final Spaceships global) {
        global.sb.query(SpatialQueries.range(getAABB(), global.range), new SpatialSetVisitor<Spaceship>() {
            @Override
            public void visit(Set<Spaceship> result) {
                System.out.println("Seeing " + result.size());

                try {
                    System.out.println("Before: " + x + ", " + y + " AABB: " + global.sb.getElement(token).getBounds());
                    System.out.println("Velocity: " + vx + ", " + vy);
                    move(global);
                    global.sb.update(token, getAABB());
                    System.out.println("After: " + x + ", " + y + " AABB: " + global.sb.getElement(token).getBounds());
                } catch (Exception e) {
                    System.err.println("Exc:" + e.getMessage());
                    e.printStackTrace();
                } finally {
                    System.out.println("done");
                }

            }

        });
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
