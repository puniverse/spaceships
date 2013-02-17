/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships;

import co.paralleluniverse.spacebase.AABB;
import co.paralleluniverse.spacebase.SpatialQuery;

    class LineQuery<T> implements SpatialQuery<T> {
        private AABB lineAABB;
        private double a, b, norm;
        private double x0,x1,y0,y1;

        public LineQuery(double x0,double x1,double y0,double y1) {
            this.x0 = x0;
            this.x1 = x1;
            this.y0 = y0;
            this.y1 = y1;
            double minX, maxX, minY, maxY;
            if (x0 < x1) {
                minX = x0;
                maxX = x1;
            } else {
                minX = x1;
                maxX = x0;
            }
            if (y0 < y1) {
                minY = y0;
                maxY = y1;
            } else {
                minY = y1;
                maxY = y0;
            }            
            this.lineAABB = AABB.create(minX,maxX,minY,maxY);
            a = (y1 - y0) / (x1 - x0);
            b = y1 - a * x1;
            norm = Math.sqrt(a * a + 1);
        }

        @Override
        public SpatialQuery.Result queryContainer(AABB aabb) {
            if (lineAABB.contains(aabb) || lineAABB.intersects(aabb))
                return SpatialQuery.Result.SOME;
            return SpatialQuery.Result.NONE;
        }

        @Override
        public boolean queryElement(AABB aabb, T elem) {
            if (!lineAABB.intersects(aabb))
                return false;
            double dist = Math.abs(a * aabb.min(0) - aabb.min(1) + b) / norm;
            if (dist<5)
                return true;
            return false;
        }
    }
