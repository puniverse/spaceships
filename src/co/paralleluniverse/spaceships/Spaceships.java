/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships;

import co.paralleluniverse.spacebase.AABB;
import co.paralleluniverse.spacebase.Debug;
import co.paralleluniverse.spacebase.ElementUpdater;
import co.paralleluniverse.spacebase.MutableAABB;
import co.paralleluniverse.spacebase.SpaceBase;
import co.paralleluniverse.spacebase.SpaceBaseBuilder;
import co.paralleluniverse.spacebase.SpaceBaseExecutors;
import co.paralleluniverse.spacebase.SpatialJoinVisitor;
import co.paralleluniverse.spacebase.SpatialModifyingVisitor;
import co.paralleluniverse.spacebase.SpatialQueries;
import co.paralleluniverse.spacebase.SpatialToken;
import co.paralleluniverse.spacebase.Sync;
import co.paralleluniverse.spaceships.render.GLPort;
import java.io.FileReader;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class Spaceships {
    public static Spaceships spaceships;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        System.out.println("COMPILER: " + System.getProperty("java.vm.name"));
        System.out.println("VERSION: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("PROCESSORS: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        Properties props = new Properties();
        props.load(new FileReader("spaceships.properties"));

        System.out.println("MARKER: " + props.getProperty("MARKER"));

        // dumpAfter(180);

        System.out.println("Initializing...");
        spaceships = new Spaceships(props);

        System.out.println("Running...");
        try {
            spaceships.run();
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
            dump();
            System.exit(1);
        }
    }
    //
    private final GLPort.Toolkit toolkit;
    public final int mode;
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
        double b = Double.parseDouble(props.getProperty("world-length", "20000"));
        this.bounds = createDimAABB(-b / 2, b / 2, -b / 2, b / 2, -b / 2, b / 2);
        this.mode = Integer.parseInt(props.getProperty("mode", "1"));
        this.N = Integer.parseInt(props.getProperty("N", "10000"));
        this.speedVariance = Double.parseDouble(props.getProperty("speed-variance", "1"));
        this.range = Double.parseDouble(props.getProperty("radar-range", "10"));

        System.out.println("===== MODE: " + mode + " =======");
        System.out.println("World bounds: " + bounds);
        System.out.println("N: " + N);

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
            ships[i] = Spaceship.create(this);

        this.sb = initSpaceBase(props);
        toolkit = GLPort.Toolkit.valueOf(props.getProperty("ui-toolkit", "NEWT").toUpperCase());

        System.out.println("UI Toolkit: " + toolkit);
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
            builder.setExecutor(SpaceBaseExecutors.parallel(parallelism));
        else
            builder.setExecutor(SpaceBaseExecutors.concurrent(parallelism));

        if (optimistic)
            builder.setOptimisticLocking(optimisticHeight, optimisticRetryLimit);
        else
            builder.setPessimisticLocking();

        builder.setDimensions(dim);

        builder.setSinglePrecision(singlePrecision).setCompressed(compressed);
        builder.setNodeWidth(nodeWidth);

        builder.setMonitoringType(SpaceBaseBuilder.MonitorType.JMX); // (SpaceBaseBuilder.MonitorType.METRICS); // 

        final SpaceBase<Spaceship> space = builder.build("base1");
        return space;
    }

    private void run() throws Exception {
        {
            System.out.println("Inserting " + N + " spaceships");
            long start = System.nanoTime();
            for (int i = 0; i < N; i++) {
                final Spaceship s = ships[i];
                MutableAABB aabb = MutableAABB.create(dim);
                s.getAABB(aabb);
                final SpatialToken token = sb.insert(s, aabb);
                token.join();
                s.setToken(token);
            }
            System.out.println("Inserted " + N + " things in " + millis(start));
        }

//        System.out.println("Sleeping for 5 seconds....");
//        Thread.sleep(5000);

        GLPort port = new GLPort(toolkit, N, sb, bounds);

        sb.setCurrentThreadAsynchronous(async);
        for (;;) {
            long start = System.nanoTime();

            if (mode == 1) {
                sb.join(SpatialQueries.distance(range), new SpatialJoinVisitor<Spaceship, Spaceship>() {
                    @Override
                    public void visit(Spaceship elem1, SpatialToken token1, Spaceship elem2, SpatialToken token2) {
                        elem1.incNeighbors();
                        elem2.incNeighbors();
                    }
                }).join();

                if (sb.getQueueLength() > 20)
                    System.out.println("???");

                System.out.println("XXX 00: " + millis(start));

                updateAll();
            } else {
                for (int i = 0; i < N; i++) {
                    final Spaceship s = ships[i];
                    final Sync sync = s.run(Spaceships.this);
                    // sync.join();
                }

                if(mode == 4)
                    updateAll();
            }

            System.out.println("XXX 11: " + millis(start));

            sb.joinAllPendingOperations();

            System.out.println("XXX 22: " + millis(start));

            final int ql = sb.getQueueLength();
            while (sb.getQueueLength() > 20) {
                Thread.sleep(5);
            }
            System.out.println("XXX: " + millis(start) + " ql: " + ql + " ql2: " + sb.getQueueLength());
            
            // System.out.println("==== " + sb.size());
        }
    }

    private void updateAll() {
        sb.queryForUpdate(SpatialQueries.ALL_QUERY, new SpatialModifyingVisitor<Spaceship>() {
            @Override
            public void visit(ElementUpdater<Spaceship> update) {
                final Spaceship spaceship = update.elem();
                spaceship.move(Spaceships.this);
                update.update(spaceship.getAABB());
            }

            @Override
            public void done() {
            }
        });
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

    private static void dumpAfter(final long seconds) {
        if (!Debug.isDebug())
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(seconds * 1000);
                    dump();
                } catch (InterruptedException e) {
                }
            }
        }, "DEBUG").start();
    }

    private static void dump() {
        if (Debug.isDebug())
            Debug.getGlobalFlightRecorder().dump(Debug.getDumpFile());
    }
}
