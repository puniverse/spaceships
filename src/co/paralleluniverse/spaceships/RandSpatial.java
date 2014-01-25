package co.paralleluniverse.spaceships;

import co.paralleluniverse.spacebase.AABB;
import co.paralleluniverse.spacebase.MutableAABB;
import static java.lang.Math.*;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author pron
 */
public class RandSpatial {

//    private final Random random;
//
//    public RandSpatial(long seed) {
//        random = new Random(seed);
//    }
//
//    public RandSpatial() {
//        random = ThreadLocalRandom.current();
//    }

    public Random getRandom() {
        return ThreadLocalRandom.current();
    }

    private MutableAABB floatify(MutableAABB aabb) {
        for (int d = 0; d < aabb.dims(); d++) {
            aabb.min(d, (float) aabb.min(d));
            aabb.max(d, (float) aabb.max(d));
        }
        return aabb;
    }

    public AABB randomAABB(AABB bounds) {
        MutableAABB aabb = AABB.create(bounds.dims());
        for (int i = 0; i < bounds.dims(); i++) {
            double a = randRange(bounds.min(i), bounds.max(i));
            double b = randRange(bounds.min(i), bounds.max(i));
            aabb.min(i, (float) min(a, b));
            aabb.max(i, (float) max(a, b));
        }

        return floatify(aabb);
    }

    public AABB randomAABB(AABB bounds, double expSize, double variance) {
        final Random random = getRandom();
        MutableAABB aabb = AABB.create(bounds.dims());
        for (int i = 0; i < bounds.dims(); i++) {
            double tmp = random.nextGaussian();
            double size = (tmp*tmp) * variance + expSize;
            if(expSize > 0 && size == 0)
                size = 0.01;
            double a = randRange(bounds.min(i), bounds.max(i) - size);
            aabb.min(i, (float) a);
            aabb.max(i, (float) (a + size));
        }

        return floatify(aabb);
    }

    public AABB randomPoint(AABB bounds) {
        return randomAABB(bounds, 0, 0);
    }

    public double randRange(double min, double max) {
        double r = getRandom().nextDouble();
        return (float)(r * (max - min) + min);
    }

    public synchronized void setSeed(long seed) {
        getRandom().setSeed(seed);
    }

    public long nextLong() {
        return getRandom().nextLong();
    }

    public int nextInt(int n) {
        return getRandom().nextInt(n);
    }

    public int nextInt() {
        return getRandom().nextInt();
    }

    public double nextGaussian() {
        return getRandom().nextGaussian();
    }

    public float nextFloat() {
        return getRandom().nextFloat();
    }

    public double nextDouble() {
        return getRandom().nextDouble();
    }

    public void nextBytes(byte[] bytes) {
        getRandom().nextBytes(bytes);
    }

    public boolean nextBoolean() {
        return getRandom().nextBoolean();
    }
}
