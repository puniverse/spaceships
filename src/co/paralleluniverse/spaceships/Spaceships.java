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
import co.paralleluniverse.spacebase.SpatialModifyingVisitor;
import co.paralleluniverse.spacebase.SpatialQueries;
import co.paralleluniverse.spacebase.SpatialToken;
import co.paralleluniverse.spacebase.Sync;
import co.paralleluniverse.spaceships.render.GLPort;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;
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
    public static final int POSTPONE_GLPORT_TILLE_SB_CYCLE_UNDER_X_MILLIS = 500;

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
    private static final int CLEANUP_THREADS = 2;
    private final File metricsDir;
    private final PrintStream configStream;
    private final PrintStream timeStream;
    private final GLPort.Toolkit toolkit;
    public final int mode;
    private final int N;
    private final int dim;
    public final AABB bounds;
    public final double speedVariance;
    public final boolean parallel;
    public final boolean async;
    public final double range;
    private final ThreadPoolExecutor executor;
    public final RandSpatial random;
    private final ArrayList<Spaceship> ships = new ArrayList<>(10000);
    public final SpaceBase<Spaceship> sb;
    public final boolean extrapolate;
    private long cycleStart;
    private GLPort port = null;

    public Spaceships(Properties props) throws Exception {
        this.dim = 2;
        this.parallel = Boolean.parseBoolean(props.getProperty("parallel", "false"));
        this.async = Boolean.parseBoolean(props.getProperty("async", "true"));
        double b = Double.parseDouble(props.getProperty("world-length", "20000"));
        this.bounds = createDimAABB(-b / 2, b / 2, -b / 2, b / 2, -b / 2, b / 2);
        this.mode = Integer.parseInt(props.getProperty("mode", "1"));
        this.N = Integer.parseInt(props.getProperty("N", "10000"));
        this.speedVariance = Double.parseDouble(props.getProperty("speed-variance", "1"));
        this.range = Double.parseDouble(props.getProperty("radar-range", "10"));
        this.extrapolate = Boolean.parseBoolean(props.getProperty("extrapolate", "true"));

        if (props.getProperty("dir") != null) {
            this.metricsDir = new File(System.getProperty("user.home") + "/" + props.getProperty("dir"));

            if (metricsDir.isDirectory()) {
                for (File file : metricsDir.listFiles())
                    file.delete();
            }
            metricsDir.mkdir();

            final File configFile = new File(metricsDir, "config.txt");
            this.configStream = new PrintStream(new FileOutputStream(configFile), true);

            final File timeFile = new File(metricsDir, "times.csv");
            this.timeStream = new PrintStream(new FileOutputStream(timeFile), true);
        } else {
            metricsDir = null;
            configStream = null;
            timeStream = null;
        }

        println("===== MODE: " + mode + ": " + Spaceship.description(mode) + " =======");
        println("World bounds: " + bounds);
        println("N: " + N);

        if (!parallel) {
            final int numThreads = Integer.parseInt(props.getProperty("parallelism", "2")) - CLEANUP_THREADS;
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
        } else
            this.executor = null;

        this.random = new RandSpatial();

        for (int i = 0; i < N; i++)
            ships.add(Spaceship.create(this));

        this.sb = initSpaceBase(props);
        toolkit = GLPort.Toolkit.valueOf(props.getProperty("ui-component", "NEWT").toUpperCase());

        println("UI Component: " + toolkit);
    }

    public void replaceSpaceship(Spaceship toRemove, Spaceship s) {
        ships.set(ships.indexOf(toRemove), s);
        sb.delete(toRemove.getToken());
        s.setToken(sb.insert(s, s.getAABB()));
    }

    private SpaceBase<Spaceship> initSpaceBase(Properties props) {
        final boolean optimistic = Boolean.parseBoolean(props.getProperty("optimistic", "true"));
        final int optimisticHeight = Integer.parseInt(props.getProperty("optimistic-height", "1"));
        final int optimisticRetryLimit = Integer.parseInt(props.getProperty("optimistic-retry-limit", "3"));
        final int parallelism = Integer.parseInt(props.getProperty("parallelism", "2"));
        final boolean compressed = Boolean.parseBoolean(props.getProperty("compressed", "false"));
        final boolean singlePrecision = Boolean.parseBoolean(props.getProperty("single-precision", "false"));
        final int nodeWidth = Integer.parseInt(props.getProperty("node-width", "10"));

        println("Parallel: " + parallel);
        println("Parallelism: " + parallelism);
        println("Optimistic: " + optimistic);
        println("Optimistic height: " + optimisticHeight);
        println("Optimistic retry limit: " + optimisticRetryLimit);
        println("Node width: " + nodeWidth);
        println("Compressed: " + compressed);
        println("Single precision: " + singlePrecision);
        println();

        SpaceBaseBuilder builder = new SpaceBaseBuilder();

        if (parallel)
            builder.setExecutor(SpaceBaseExecutors.parallel(parallelism));
        else
            builder.setExecutor(SpaceBaseExecutors.concurrent(CLEANUP_THREADS));

        builder.setQueueBackpressure(1000);

        if (optimistic)
            builder.setOptimisticLocking(optimisticHeight, optimisticRetryLimit);
        else
            builder.setPessimisticLocking();

        builder.setDimensions(dim);

        builder.setSinglePrecision(singlePrecision).setCompressed(compressed);
        builder.setNodeWidth(nodeWidth);

        builder.setMonitoringType(SpaceBaseBuilder.MonitorType.JMX);
        if (metricsDir != null)
            com.yammer.metrics.reporting.CsvReporter.enable(metricsDir, 1, TimeUnit.SECONDS);

        final SpaceBase<Spaceship> space = builder.build("base1");
        return space;
    }

    private void run() throws Exception {
        {
            System.out.println("Inserting " + N + " spaceships");
            long insrertStart = System.nanoTime();
            for (Spaceship s : ships) {
                insertSpaceship(s);
            }
            System.out.println("Inserted " + N + " things in " + millis(insrertStart));
        }

        if (timeStream != null)
            timeStream.println("# time, millis, millis1, millis0");

        sb.setCurrentThreadAsynchronous(async);
        for (int k = 0;; k++) {
            cycleStart = System.nanoTime();
            float millis0, millis1, millis;

            if (mode == 1) {
            } else {
                for (final Spaceship s : ships) {
                    if (executor == null) {
                        final Sync sync = s.run(Spaceships.this);
                        // sync.join();
                    } else {
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final Sync sync = s.run(Spaceships.this);
                                    // sync.join();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    System.exit(1);
                                }
                            }
                        });
                    }
                }
            }

            if (mode <= 2) {
                //sb.joinAllPendingOperations();
                millis0 = millis(cycleStart);
                //out.println("XXX 00: " + millis(start));
                updateAll();
            } else
                millis0 = 0;

            millis1 = millis(cycleStart);
            System.out.println("XXX 11: " + millis(cycleStart));

            if (millis1 < 10)
                Thread.sleep(10 - (int) millis1);

            millis = millis(cycleStart);
            if (timeStream != null)
                timeStream.println(k + "," + millis + "," + millis1 + "," + millis0);

            if (port == null & millis < POSTPONE_GLPORT_TILLE_SB_CYCLE_UNDER_X_MILLIS)
                port = new GLPort(toolkit, N, Spaceships.this, bounds);

            System.out.println("XXX: " + millis + (executor != null ? " executorQueue: " + executor.getQueue().size() : ""));
        }
    }

    public long getCycleStart() {
        return cycleStart;
    }

    private void updateAll() {
        sb.queryForUpdate(SpatialQueries.ALL_QUERY, new SpatialModifyingVisitor<Spaceship>() {
            @Override
            public void visit(ElementUpdater<Spaceship> update) {
                final long currentTime = currentTime();
                final Spaceship spaceship = update.elem();
                spaceship.move(Spaceships.this, currentTime);
                update.update(spaceship.getAABB());
            }

            @Override
            public void done() {
            }
        });
    }

    long currentTime() {
        return System.currentTimeMillis();
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

    private static void dump() {
        if (Debug.isDebug())
            Debug.getGlobalFlightRecorder().dump(Debug.getDumpFile());
    }

    private void println() {
        println("");
    }

    private void println(String str) {
        if (configStream != null)
            configStream.println(str);
        System.out.println(str);
    }

    public void insertSpaceship(Spaceship s) throws InterruptedException {
        if (!ships.contains(s))
            ships.add(s);
        MutableAABB aabb = MutableAABB.create(dim);
        s.getAABB(aabb);
        final SpatialToken token = sb.insert(s, aabb);
        token.join();
        s.setToken(token);
    }
}
