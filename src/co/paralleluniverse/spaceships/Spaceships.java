/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships;

import co.paralleluniverse.spacebase.AABB;
import co.paralleluniverse.spacebase.MutableAABB;
import co.paralleluniverse.spacebase.SpaceBase;
import co.paralleluniverse.spacebase.SpaceBaseBuilder;
import co.paralleluniverse.spaceships.render.GLPort;
import java.io.FileReader;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class Spaceships {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        System.out.println("COMPILER: " + System.getProperty("java.vm.name"));
        System.out.println("VERSION: " + System.getProperty("java.version"));
        System.out.println();

        Properties props = new Properties();
        props.load(new FileReader("spaceships.properties"));

        System.out.println("MARKER: " + props.getProperty("MARKER"));

        System.out.println("Initializing...");
        final Spaceships spaceships = new Spaceships(props);
        System.out.println("Running...");
        spaceships.run();
        Thread.sleep(Long.MAX_VALUE);
    }

    private final int N;
    private final int dim;
    public final AABB bounds;
    public final double speedVariance;
    public final boolean async;
    public final double range;
    private final ExecutorService executor;
    public final RandSpatial random;
    private final Spaceship[] ships;
    public final SpaceBase<Spaceship> sb;

    public Spaceships(Properties props) {
        this.dim = 2;
        this.async = Boolean.parseBoolean(props.getProperty("async", "true"));
        this.bounds = createDimAABB(-10000, 10000, -10000, 10000, -10000, 10000);
        this.N = Integer.parseInt(props.getProperty("N", "10000"));
        this.speedVariance = Double.parseDouble(props.getProperty("speed-variance", "1"));
        this.range = Double.parseDouble(props.getProperty("radar-range", "10"));

        final int numThreads = Integer.parseInt(props.getProperty("io-threads", "2"));
        this.executor = new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    executor.getQueue().put(r);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }

        });

        this.random = new RandSpatial();

        this.ships = new Spaceship[N];
        for (int i = 0; i < N; i++)
            ships[i] = new Spaceship(this);

        this.sb = initSpaceBase(props);

    }

    private SpaceBase<Spaceship> initSpaceBase(Properties props) {
        final boolean optimistic = Boolean.parseBoolean(props.getProperty("optimistic", "true"));
        final int optimisticHeight = Integer.parseInt(props.getProperty("optimistic-height", "1"));
        final int optimisticRetryLimit = Integer.parseInt(props.getProperty("optimistic-retry-limit", "3"));
        final boolean parallel = Boolean.parseBoolean(props.getProperty("parallel", "false"));
        final int parallelism = Integer.parseInt(props.getProperty("parallelism", "2"));
        final boolean compressed = Boolean.parseBoolean(props.getProperty("compressed", "false"));
        final boolean singlePrecision = Boolean.parseBoolean(props.getProperty("single-precision", "false"));
        final int nodeWidth = Integer.parseInt(props.getProperty("node-width", "10"));

        System.out.println("Parallel: " + parallel);
        System.out.println("Parallelism: " + parallelism);
        System.out.println("Optimistic: " + optimistic);
        System.out.println("Optimistic height: " + optimisticHeight);
        System.out.println("Optimistic retry limit: " + optimisticRetryLimit);
        System.out.println("Node width: " + nodeWidth);
        System.out.println("Compressed: " + compressed);
        System.out.println("Single precision: " + singlePrecision);
        System.out.println();

        SpaceBaseBuilder builder = new SpaceBaseBuilder();

        if (parallel)
            builder.setParallelMode(parallelism);
        else
            builder.setConcurrentMode(parallelism);

        if (optimistic)
            builder.setOptimisticLocking(optimisticHeight, optimisticRetryLimit);
        else
            builder.setPessimisticLocking();

        builder.setDimensions(dim);

        builder.setSinglePrecision(singlePrecision).setCompressed(compressed);
        builder.setNodeWidth(nodeWidth);

        final SpaceBase<Spaceship> space = builder.build("base1");
        return space;
    }

    private void run() {
        {
            System.out.println("Inserting " + N + " spaceships");
            long start = System.nanoTime();
            for (int i = 0; i < N; i++) {
                final Spaceship s = ships[i];
                MutableAABB aabb = MutableAABB.create(dim);
                s.getAABB(aabb);
                s.setToken(sb.insert(s, aabb));
            }
            System.out.println("Inserted " + N + " things in " + millis(start));
        }

        GLPort port = new GLPort(N, sb);

        for (;;) {
            long start = System.nanoTime();
            for (int i = 0; i < N; i++) {
                final Spaceship s = ships[i];
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        sb.setCurrentThreadAsynchronous(async);
                        s.run(Spaceships.this);
                    }

                });
            }
            millis(start);
        }
    }

    private float millis(long nanoStart) {
        return (float) (System.nanoTime() - nanoStart) / 1000000;
    }

    private AABB createDimAABB(double minx, double maxx, double miny, double maxy, double minz, double maxz) {
        if (dim == 2)
            return AABB.create((float) minx, (float) maxx, (float) miny, (float) maxy);
        if (dim == 3)
            return AABB.create((float) minx, (float) maxx, (float) miny, (float) maxy, (float) minz, (float) maxz);
        else {
            throw new AssertionError();
        }
    }

}
