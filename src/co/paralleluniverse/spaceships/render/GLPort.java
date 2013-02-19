/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships.render;

import co.paralleluniverse.spacebase.AABB;
import static co.paralleluniverse.spacebase.AABB.X;
import static co.paralleluniverse.spacebase.AABB.Y;
import co.paralleluniverse.spacebase.MutableAABB;
import co.paralleluniverse.spacebase.SpaceBase;
import co.paralleluniverse.spacebase.SpatialQueries;
import co.paralleluniverse.spacebase.SpatialQuery;
import co.paralleluniverse.spacebase.SpatialToken;
import co.paralleluniverse.spacebase.SpatialVisitor;
import co.paralleluniverse.spaceships.Spaceship;
import co.paralleluniverse.spaceships.Spaceships;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import java.awt.Component;
import java.awt.Frame;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Collections;
import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

/**
 * See:
 * http://www.lighthouse3d.com/tutorials/glsl-core-tutorial/3490-2/
 * http://www.arcsynthesis.org/gltut/Positioning/Tut07%20Shared%20Uniforms.html
 * http://www.jotschi.de/?p=427
 *
 * @author pron
 */
public class GLPort implements GLEventListener {
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 700;
    public static final double ZOOM_UNIT = 0.1;
    public static final int ANIMATION_TIME = 200;
    private long lastQuery = 0;
    private Collection<Object> lastRes = null;
    private Texture spaceshipTex;
    private Texture explosionTex;
    private ShaderProgram shaderProgram;
    private long portUpdateTime = 0;
    private double inProcessPortExtention;
    private int drawableWidth;
    private int drawableHeight;

    private MutableAABB getCurrentPort(final long ct) {
        if (inProcessPortExtention==0) return port;
        MutableAABB currentPort = MutableAABB.create(2);
        final double width = port.max(X) - port.min(X);
        final double height = port.max(Y) - port.min(Y);
        final double ratio = height / width;
        double animation = Math.min(1.0,(double)(ct-portUpdateTime)/ANIMATION_TIME);
        currentPort.min(X, port.min(X) - animation * inProcessPortExtention);
        currentPort.min(Y, port.min(Y) - animation * inProcessPortExtention * ratio);
        currentPort.max(X, port.max(X) + animation * inProcessPortExtention);
        currentPort.max(Y, port.max(Y) + animation * inProcessPortExtention * ratio);
        return currentPort;
    }

    private void fixPortToNow(final long ct, boolean onlyIfFinished) {
        final double width = port.max(X) - port.min(X);
        final double height = port.max(Y) - port.min(Y);
        final double ratio = height / width;
        double animation = Math.min(1.0,(double)(ct-portUpdateTime)/ANIMATION_TIME);
        if (onlyIfFinished & animation<1.0) return;
        double kk = width + 2 * inProcessPortExtention;
        port.min(X, port.min(X) - animation * inProcessPortExtention);
        port.min(Y, port.min(Y) - animation * inProcessPortExtention * ratio);
        port.max(X, port.max(X) + animation * inProcessPortExtention);
        port.max(Y, port.max(Y) + animation * inProcessPortExtention * ratio);
        inProcessPortExtention -= animation * inProcessPortExtention;
        System.out.println("KKKK2 "+kk+"\t"+((port.max(X)-port.min(X)+2*inProcessPortExtention)));
        portUpdateTime = ct;
    }

    public enum Toolkit {
        NEWT, NEWT_CANVAS, AWT
    };
    private final Toolkit TOOLKIT;
    //
    private static final float KEY_PRESS_TRANSLATE = 10.0f;
    private final Object window;
    private final int maxItems;
    private final SpaceBase<Spaceship> sb;
    private final AABB bounds;
    private MutableAABB port = MutableAABB.create(2);
    private ProgramState shaderState;
    private VAO vao;
    private VBO vertices;
    private VBO colors;
    private PMVMatrix pmv = new PMVMatrix();
    private float x = 1.0f;
    private final FPSAnimator animator;
    private final Spaceships global;

    static {
        GLProfile.initSingleton();
    }

    public GLPort(Toolkit toolkit, int maxItems, Spaceships global, AABB bounds) {
        TOOLKIT = toolkit;
        this.maxItems = maxItems;
        this.global = global;
        this.sb = global.sb;
        this.bounds = bounds;

        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilitiesImmutable glcaps = (GLCapabilitiesImmutable) new GLCapabilities(glp);
        final GLAutoDrawable drawable;

        if (TOOLKIT == Toolkit.NEWT || TOOLKIT == Toolkit.NEWT_CANVAS) {
            final GLWindow newt = GLWindow.create(glcaps);

            final NewtListener listener = new NewtListener();
            newt.addKeyListener(listener);
            newt.addMouseListener(listener);

            drawable = newt;
        } else {
            final GLCanvas glCanvas = new GLCanvas(glcaps);

            final AwtListener listener = new AwtListener();
            glCanvas.addKeyListener(listener);
            glCanvas.addMouseListener(listener);
            glCanvas.addMouseMotionListener(listener);
            glCanvas.addMouseWheelListener(listener);

            drawable = glCanvas;
        }

        drawable.addGLEventListener(this);
        animator = new FPSAnimator(drawable, 60);

        if (TOOLKIT == Toolkit.NEWT) {
            final GLWindow window = (GLWindow) drawable;

            window.addWindowListener(new com.jogamp.newt.event.WindowAdapter() {
                @Override
                public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent arg0) {
                    animator.stop();
                    System.exit(0);
                }
            });
            window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            window.setTitle("Spaceships");
            window.setVisible(true);
            this.window = window;
        } else {
            final Component canvas;

            if (TOOLKIT == Toolkit.NEWT_CANVAS)
                canvas = new NewtCanvasAWT((GLWindow) drawable);
            else
                canvas = (GLCanvas) drawable;

            final Frame window = new Frame();

            window.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowevent) {
                    animator.stop();
                    window.remove(canvas);
                    window.dispose();
                    System.exit(0);
                }
            });

            window.add(canvas);
            window.pack();
            canvas.requestFocusInWindow();
            window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            window.setTitle("Spaceships");
            window.setVisible(true);
            this.window = window;
        }

        animator.start();
    }

    private void setTitle(String title) {
        if (TOOLKIT == Toolkit.NEWT)
            ((GLWindow) window).setTitle(title);
        else
            ((Frame) window).setTitle(title);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        drawable.setGL(new DebugGL3(drawable.getGL().getGL3()));

        final GL3 gl = drawable.getGL().getGL3();
        try {
            // This icon is under the LGPL license
            // by Everaldo Coelho
            InputStream stream = new FileInputStream("spaceship.png");
            TextureData data = TextureIO.newTextureData(GLProfile.get(GLProfile.GL3),stream, false, "png");
            spaceshipTex = TextureIO.newTexture(data);
        }
        catch (IOException exc) {
            exc.printStackTrace();
            System.exit(1);
        }
        try {
            // This icon is under the license "Creative Commons Attribution-Share Alike 3.0"
            // by Christian "Crystan" Hoffmann
            InputStream stream = new FileInputStream("explosion.png");
            TextureData data = TextureIO.newTextureData(GLProfile.get(GLProfile.GL3),stream, false, "png");
            explosionTex = TextureIO.newTexture(data);
        }
        catch (IOException exc) {
            exc.printStackTrace();
            System.exit(1);
        }

        drawableWidth = drawable.getWidth();
        drawableHeight = drawable.getHeight();
        port.min(X, -drawable.getWidth() / 2);
        port.max(X, drawable.getWidth() / 2);
        port.min(Y, -drawable.getHeight() / 2);
        port.max(Y, drawable.getHeight() / 2);
        //gl.glEnable(gl.GL_VERTEX_PROGRAM_POINT_SIZE);
        gl.glViewport(0, 0, (int) (port.max(X) - port.min(X)), (int) (port.max(Y) - port.min(Y)));
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);

        ShaderCode vertexShader = ShaderCode.create(gl, gl.GL_VERTEX_SHADER, this.getClass(), "shader", null, "vertex", false);
        ShaderCode geometryShader = ShaderCode.create(gl, gl.GL_GEOMETRY_SHADER, this.getClass(), "shader", null, "geometry", false);
        ShaderCode fragmentShader = ShaderCode.create(gl, gl.GL_FRAGMENT_SHADER, this.getClass(), "shader", null, "fragment", false);

        if (!vertexShader.compile(gl, System.err))
            throw new GLException("Couldn't compile shader: " + vertexShader);
        if (!geometryShader.compile(gl, System.err))
            throw new GLException("Couldn't compile shader: " + geometryShader);
        if (!fragmentShader.compile(gl, System.err))
            throw new GLException("Couldn't compile shader: " + fragmentShader);

        shaderProgram = new ShaderProgram();
        shaderProgram.add(gl, vertexShader, System.err);
        shaderProgram.add(gl, geometryShader, System.err);
        shaderProgram.add(gl, fragmentShader, System.err);
        if (!shaderProgram.link(gl, System.err))
            throw new GLException("Couldn't link program: " + shaderProgram);

        this.shaderState = new ProgramState(gl, shaderProgram);
        shaderState.bind(gl);

        //portToMvMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmv.glLoadIdentity();

        this.vao = shaderState.createVAO(gl);
        vao.bind(gl);

        this.vertices = new VBO(gl, 2, gl.GL_FLOAT, false, maxItems, gl.GL_DYNAMIC_DRAW);
        this.colors = new VBO(gl, 3, gl.GL_FLOAT, false, maxItems, gl.GL_DYNAMIC_DRAW);

        vao.setVertex(gl, "in_Position", vertices);
        vao.setVertex(gl, "in_Vertex", colors);

//        shaderState.setUniform(gl, "myTexture", myTexture);
        //shaderState.createUBO(gl, "MatrixBlock");
        //shaderState.getUBO("MatrixBlock").bind(gl).set(gl, "PMatrix", 4, 4, pmv.glGetMvMatrixf());

        shaderState.unbind(gl);
    }

    private void portToMvMatrix(MutableAABB cp) {
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glScalef((float) (2.0 / (cp.max(X) - cp.min(X))), (float) (2.0 / (cp.max(Y) - cp.min(Y))), 1.0f);
        pmv.glTranslatef((float) (-(cp.max(X) + cp.min(X)) / 2.0), (float) (-(cp.max(Y) + cp.min(Y)) / 2.0), 0f);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();

        shaderState.bind(gl);
        shaderState.destroy(gl);
        vertices.destroy(gl);
    }

    public Collection<Object> query(SpatialQuery<? super Spaceship> query) {
        try {
            final Collection<Object> resultSet =  sb.createCollection();
//            final ConcurrentHashMap resultSet = new ConcurrentHashMap(100);
            sb.query(query, new SpatialVisitor<Spaceship>() {
                @Override
                public void visit(Spaceship elem, SpatialToken token) {
                    resultSet.add(elem);
                }

                @Override
                public void done() {
                }
            }).join();

            return Collections.unmodifiableCollection(resultSet);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
//        System.out.println("KKKK START");
        final GL3 gl = drawable.getGL().getGL3();
        shaderState.bind(gl);
        vao.bind(gl);
        int spaceshipLoc = gl.glGetUniformLocation(shaderProgram.program(), "spaceshipTex");
        gl.glUniform1i(spaceshipLoc, 0);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, spaceshipTex.getTextureObject(gl));

        int explosionLoc = gl.glGetUniformLocation(shaderProgram.program(), "explosionTex");
        gl.glUniform1i(explosionLoc, 1);
        gl.glActiveTexture(GL.GL_TEXTURE0 + 1);
        gl.glBindTexture(GL.GL_TEXTURE_2D, explosionTex.getTextureObject(gl));
//        spaceshipTex.enable(gl);
//        spaceshipTex.bind(gl);
//        gl.glActiveTexture(GL.GL_TEXTURE1);
//        explosionTex.enable(gl);
//        explosionTex.bind(gl);
//        shaderState.setUniform(gl, "myTexture", spaceshipTex.getTextureObject(gl));

        vertices.clear();
        colors.clear();
        final FloatBuffer verticesb = (FloatBuffer) vertices.getBuffer();
        final FloatBuffer colorsb = (FloatBuffer) colors.getBuffer();
        float col, head;
        long ct = System.currentTimeMillis();
        fixPortToNow(ct,true);
        MutableAABB currentPort = getCurrentPort(ct);
        portToMvMatrix(currentPort);
        double m = global.range *2;

        if (ct - lastQuery > 100) {
            lastQuery = ct;
            lastRes = query(SpatialQueries.contained(AABB.create(currentPort.min(X)-m,currentPort.max(X)+m,currentPort.min(Y)-m,currentPort.max(Y)+m)));
        }
        if (!global.extrapolate)
            ct = lastQuery;
        double[] pos;
        for (Object o : lastRes) {
            Spaceship s = (Spaceship) o;
            if (s.getLastMoved() > 0) {
                pos = s.getCurrentPosition(ct);
                col = Math.min(1.0f, 0.1f + (float) s.getNeighbors() / 10.0f);
                verticesb.put((float)pos[0]);
                verticesb.put((float)pos[1]);
                if (s.getBlowTime()>0) {
                    colorsb.put(Math.min(1.0f, (ct-s.getBlowTime())/500f));                                        
                } else                     
                    colorsb.put(0);
                colorsb.put((float) s.getCurrentHeading(ct));
                colorsb.put(ct - s.getShootTime() < 100 ? (float)s.getShootLength() : 0f);
            }
        }

        vertices.flip();
        colors.flip();

        int numElems = verticesb.limit() / 2;
//        System.out.println("KKKK SHIPS\t" + numElems + "\t" + sb.size() + "\t" + realQuery);
        vertices.write(gl, 0, numElems);
        colors.write(gl, 0, numElems);

        shaderState.setUniform(gl, "in_Matrix", 4, 4, pmv.glGetMvMatrixf());
//        shaderState.setUniform(gl, "myTexture",myTexture.getTextureObject(gl));
        //shaderState.getUBO("MatrixBlock").set(gl, "PMatrix", 4, 4, pmv.glGetMvMatrixf());

        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glDrawArrays(gl.GL_POINTS, 0, numElems);

        vao.unbind(gl);
        shaderState.unbind(gl);
//        System.out.println("KKKK END");
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glViewport(0, 0, width, height);
        port.max(X, port.min(X) + (double) width / drawableWidth * (port.max(X)-port.min(X)));
        port.max(Y, port.min(Y) + (double) height / drawableHeight * (port.max(Y)-port.min(Y)));
        drawableHeight = height;
        drawableWidth = width;
        portToMvMatrix(port);
        setTitle("Spaceships " + (port.max(X) - port.min(X)) + "x" + (port.max(Y) - port.min(Y)));
    }

    private void movePort(boolean horizontal, double units) {
        //pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        final double width = port.max(X) - port.min(X);
        final double height = port.max(Y) - port.min(Y);

        if (horizontal) {
            //pmv.glTranslatef(-units * KEY_PRESS_TRANSLATE, 0f, 0f);
            if (port.min(X) + units * KEY_PRESS_TRANSLATE < bounds.min(X)) {
                port.min(X, bounds.min(X));
                port.max(X, port.min(X) + width);
            } else if (port.max(X) + units * KEY_PRESS_TRANSLATE > bounds.max(X)) {
                port.max(X, bounds.max(X));
                port.min(X, port.max(X) - width);
            } else {
                port.min(X, port.min(X) + units * KEY_PRESS_TRANSLATE);
                port.max(X, port.max(X) + units * KEY_PRESS_TRANSLATE);
            }
        } else {
            //pmv.glTranslatef(0f, -units * KEY_PRESS_TRANSLATE, 0f);
            if (port.min(Y) + units * KEY_PRESS_TRANSLATE < bounds.min(Y)) {
                port.min(Y, bounds.min(Y));
                port.max(Y, port.min(Y) + height);
            } else if (port.max(Y) + units * KEY_PRESS_TRANSLATE > bounds.max(Y)) {
                port.max(Y, bounds.max(Y));
                port.min(Y, port.max(Y) - height);
            } else {
                port.min(Y, port.min(Y) + units * KEY_PRESS_TRANSLATE);
                port.max(Y, port.max(Y) + units * KEY_PRESS_TRANSLATE);
            }
        }
        portToMvMatrix(port);
    }

        private void scalePort(boolean zoomIn, double units) {
        //pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        final long ct = System.currentTimeMillis();
        fixPortToNow(ct, false);
        final double width = port.max(X) - port.min(X);
        final double height = port.max(Y) - port.min(Y);
        final double ratio = height / width;
        final double widthToAdd = width * ZOOM_UNIT * units;
        
//        if (addToPort!=0) return; //don't scale during previous scale    
        if (zoomIn) {
            if ((width+2*(-widthToAdd +inProcessPortExtention) > 400) & (height+2*((-widthToAdd +inProcessPortExtention)*ratio) > 400 * ratio)) {
                inProcessPortExtention -= widthToAdd;
//                port = getCurrentPort(ct);
            }
        } else { // zoomout
            if ((bounds.min(X) < port.min(X) - inProcessPortExtention - widthToAdd) &
                (bounds.min(Y) < port.min(Y) - inProcessPortExtention - widthToAdd * ratio) &
                (bounds.max(X) > port.max(X) + inProcessPortExtention + widthToAdd) &
                (bounds.max(Y) > port.max(Y) + inProcessPortExtention + widthToAdd * ratio)) {
                inProcessPortExtention += widthToAdd;
//                port = getCurrentPort(ct);
//                port.min(X, port.min(X) - units * KEY_RATIO_TRANSLATE);
//                port.min(Y, port.min(Y) - units * KEY_RATIO_TRANSLATE * ratio);
//                port.max(X, port.max(X) + units * KEY_RATIO_TRANSLATE);
//                port.max(Y, port.max(Y) + units * KEY_RATIO_TRANSLATE * ratio);
            }
        }
        System.out.println("KKKK3 "+((port.max(X)-port.min(X))+2*inProcessPortExtention));
//        portToMvMatrix(port);
    }

    private void print(FloatBuffer buffer) {
        int pos = buffer.position();
        System.err.print(buffer.remaining() + ": ");
        while (buffer.position() < buffer.limit())
            System.err.print(buffer.get() + ", ");
        System.err.println();
        buffer.position(pos);
    }

    private class AwtListener implements java.awt.event.KeyListener, java.awt.event.MouseListener, java.awt.event.MouseMotionListener, java.awt.event.MouseWheelListener {
        @Override
        public void keyPressed(java.awt.event.KeyEvent e) {
            int keyCode = e.getKeyCode();
            switch (keyCode) {
                case java.awt.event.KeyEvent.VK_UP:
                    movePort(false, 1);
                    break;
                case java.awt.event.KeyEvent.VK_DOWN:
                    movePort(false, -1);
                    break;
                case java.awt.event.KeyEvent.VK_LEFT:
                    movePort(true, -1);
                    break;
                case java.awt.event.KeyEvent.VK_RIGHT:
                    movePort(true, 1);
                    break;
            }
        }

        @Override
        public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
            movePort(e.isShiftDown(), (e.isShiftDown() ? 1 : -1) * e.getWheelRotation());
        }

        @Override
        public void keyTyped(java.awt.event.KeyEvent e) {
        }

        @Override
        public void keyReleased(java.awt.event.KeyEvent e) {
        }

        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mouseReleased(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mouseEntered(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mouseDragged(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mouseMoved(java.awt.event.MouseEvent e) {
        }
    }

    private class NewtListener implements com.jogamp.newt.event.KeyListener, com.jogamp.newt.event.MouseListener {
        @Override
        public void keyPressed(com.jogamp.newt.event.KeyEvent e) {
            int keyCode = e.getKeyCode();
            final boolean zoomIn = true;
            switch (keyCode) {
                case com.jogamp.newt.event.KeyEvent.VK_UP:
                    movePort(false, 1);
                    break;
                case com.jogamp.newt.event.KeyEvent.VK_DOWN:
                    movePort(false, -1);
                    break;
                case com.jogamp.newt.event.KeyEvent.VK_LEFT:
                    movePort(true, -1);
                    break;
                case com.jogamp.newt.event.KeyEvent.VK_RIGHT:
                    movePort(true, 1);
                    break;
                case com.jogamp.newt.event.KeyEvent.VK_EQUALS:
                    scalePort(zoomIn, 1);
                    break;
                case com.jogamp.newt.event.KeyEvent.VK_MINUS:
                    scalePort(!zoomIn, 1);
                    break;
            }
        }

        @Override
        public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
            movePort(e.isShiftDown(), -1 * (e.isShiftDown() ? 1 : -1) * e.getWheelRotation());
        }

        @Override
        public void keyReleased(com.jogamp.newt.event.KeyEvent e) {
        }

        @Override
        public void keyTyped(com.jogamp.newt.event.KeyEvent e) {
        }

        @Override
        public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
        }

        @Override
        public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
        }

        @Override
        public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
        }

        @Override
        public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
        }

        @Override
        public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
        }

        @Override
        public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
        }

        @Override
        public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
        }
    }
}
