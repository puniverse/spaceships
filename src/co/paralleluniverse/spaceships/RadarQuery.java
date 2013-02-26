/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships;

import co.paralleluniverse.spacebase.AABB;
import co.paralleluniverse.spacebase.SpatialQuery;

/**
 *
 * @author eitan
 */
public class RadarQuery implements SpatialQuery<Spaceship> 
{
    private final double x,y;
    private final double dev;
    private final double range;
    private AABB aabb;
    private final double heading;

    public RadarQuery(double x, double y, double vx, double vy, double dev, double range) {
        this.x = x;
        this.y = y;
        this.dev = dev;
        this.range = range;
        this.heading = Math.atan2(vy, vx);
        double minAng = heading - dev;
        double maxAng = heading + dev;
        double x1,x2,y1,y2;
        x1 = x + range * Math.cos(minAng);        
        x2 = x + range * Math.cos(maxAng);
        y1 = y + range * Math.sin(minAng);
        y2 = y + range * Math.sin(maxAng);
        aabb = AABB.create(
                Math.min(x, Math.min(x1, x2)),
                Math.max(x, Math.max(x1, x2)),
                Math.min(y, Math.min(y1, y2)),
                Math.max(y, Math.max(y1, y2))
                );
        
    }        
    
    
    @Override
    public Result queryContainer(AABB aabb) {
            if (this.aabb.contains(aabb) || this.aabb.intersects(aabb))
                return SpatialQuery.Result.SOME;
            return SpatialQuery.Result.NONE;
    }

    @Override
    public boolean queryElement(AABB aabb, Spaceship elem) {
        double ang = Math.atan2(aabb.max(1)-y, aabb.max(0)-x);
        if (Math.abs(ang-heading)<dev) return true;
        return false;
    }
    
}
