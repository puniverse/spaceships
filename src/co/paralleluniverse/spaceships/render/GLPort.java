/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships.render;

import co.paralleluniverse.spacebase.AABB;
import static co.paralleluniverse.spacebase.AABB.X;
import static co.paralleluniverse.spacebase.AABB.Y;
import co.paralleluniverse.spacebase.ElementUpdater;
import co.paralleluniverse.spacebase.MutableAABB;
import co.paralleluniverse.spacebase.SpaceBase;
import co.paralleluniverse.spacebase.SpatialQueries;
import co.paralleluniverse.spacebase.SpatialSetVisitor;
import co.paralleluniverse.spaceships.Spaceship;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.awt.Component;
import java.awt.Frame;
import java.nio.FloatBuffer;
import java.util.Set;
import javax.media.opengl.DebugGL3;
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
 *
 * @author pron
 */
public class GLPort implements GLEventListener {
    public enum Toolkit {
        NEWT, NEWT_CANVAS, AWT
    };
    private final Toolkit TOOLKIT;
    //
    private static final float KEY_PRESS_TRANSLATE = 10.0f;
    private final int maxItems;
    private final SpaceBase<Spaceship> sb;
    private final AABB bounds;
    private final MutableAABB port = MutableAABB.create(2);
    private ProgramState shaderState;
    private VAO vao;
    private VBO vertices;
    private VBO colors;
    private PMVMatrix pmv = new PMVMatrix();
    private float x = 1.0f;
    private final FPSAnimator animator;

    static {
        GLProfile.initSingleton();
    }

    public GLPort(Toolkit toolkit, int maxItems, SpaceBase<Spaceship> sb, AABB bounds) {
        TOOLKIT = toolkit;
        this.maxItems = maxItems;
        this.sb = sb;
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
            window.setSize(300, 300);
            window.setTitle("Spaceships");
            window.setVisible(true);
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
            window.setSize(300, 300);
            window.setTitle("Spaceships");
            window.setVisible(true);
        }

        animator.start();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        drawable.setGL(new DebugGL3(drawable.getGL().getGL3()));
        //drawable.setGL(new TraceGL3(new DebugGL3(drawable.getGL().getGL3()), System.err));

        final GL3 gl = drawable.getGL().getGL3();

        port.min(X, -150);
        port.max(X, 150);
        port.min(Y, -150);
        port.max(Y, 150);
        gl.glEnable(gl.GL_VERTEX_PROGRAM_POINT_SIZE);
        gl.glViewport(0, 0, (int) (port.max(X) - port.min(X)), (int) (port.max(Y) - port.min(Y)));
        gl.glClearColor(0, 0, 0, 1);

        ShaderCode vertexShader = ShaderCode.create(gl, gl.GL_VERTEX_SHADER, this.getClass(), "shader", null, "vertex", false);
        ShaderCode fragmentShader = ShaderCode.create(gl, gl.GL_FRAGMENT_SHADER, this.getClass(), "shader", null, "fragment", false);

        if (!vertexShader.compile(gl, System.err))
            throw new GLException("Couldn't compile shader: " + vertexShader);
        if (!fragmentShader.compile(gl, System.err))
            throw new GLException("Couldn't compile shader: " + fragmentShader);

        final ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(gl, vertexShader, System.err);
        shaderProgram.add(gl, fragmentShader, System.err);
        if (!shaderProgram.link(gl, System.err))
            throw new GLException("Couldn't link program: " + shaderProgram);

        this.shaderState = new ProgramState(gl, shaderProgram);
        shaderState.bind(gl);

        portToMvMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmv.glLoadIdentity();

        this.vao = shaderState.createVAO(gl);
        vao.bind(gl);

        this.vertices = new VBO(gl, 2, gl.GL_FLOAT, false, maxItems, gl.GL_DYNAMIC_DRAW);
        this.colors = new VBO(gl, 1, gl.GL_FLOAT, false, maxItems, gl.GL_DYNAMIC_DRAW);

        vao.setVertex(gl, "in_Position", vertices);
        vao.setVertex(gl, "in_Color", colors);

        shaderState.setUniform(gl, "in_Matrix", 4, 4, pmv.glGetMvMatrixf());
        //shaderState.createUBO(gl, "MatrixBlock");
        //shaderState.getUBO("MatrixBlock").bind(gl).set(gl, "PMatrix", 4, 4, pmv.glGetMvMatrixf());

        shaderState.unbind(gl);
    }

    private void portToMvMatrix() {
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glScalef((float) (2.0 / (port.max(X) - port.min(X))), (float) (2.0 / (port.max(Y) - port.min(Y))), 1.0f);
        pmv.glTranslatef((float) (-(port.max(X) + port.min(X)) / 2.0), (float) (-(port.max(Y) + port.min(Y)) / 2.0), 0f);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();

        shaderState.bind(gl);
        shaderState.destroy(gl);
        vertices.destroy(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        try {
            final GL3 gl = drawable.getGL().getGL3();

            shaderState.bind(gl);
            vao.bind(gl);

            vertices.rewind();
            colors.rewind();
            final FloatBuffer verticesb = (FloatBuffer) vertices.getBuffer();
            final FloatBuffer colorsb = (FloatBuffer) colors.getBuffer();
            //System.out.println("XXX " + verticeb + " " + port);
            sb.query(SpatialQueries.contained(port), new SpatialSetVisitor<Spaceship>() {
                @Override
                public void visit(Set<Spaceship> resultReadOnly, Set<ElementUpdater<Spaceship>> resultForUpdate) {
                    for (Spaceship s : resultReadOnly) {
                        verticesb.put((float) s.getX());
                        verticesb.put((float) s.getY());

                        colorsb.put(Math.min(1.0f, 0.3f + (float) s.getNeighbors() / 5.0f));
                    }
                }
            }).join();

            vertices.rewind();
            colors.rewind();

            int numElems = verticesb.limit() / 2;
            vertices.write(gl, 0, numElems);
            colors.write(gl, 0, numElems);

            shaderState.setUniform(gl, "in_Matrix", 4, 4, pmv.glGetMvMatrixf());
            //shaderState.getUBO("MatrixBlock").set(gl, "PMatrix", 4, 4, pmv.glGetMvMatrixf());

            gl.glClear(gl.GL_COLOR_BUFFER_BIT);
            gl.glDrawArrays(gl.GL_POINTS, 0, numElems);

            vao.unbind(gl);
            shaderState.unbind(gl);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glViewport(0, 0, width, height);
        port.max(X, port.min(X) + width);
        port.max(Y, port.min(Y) + height);
        portToMvMatrix();
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
        portToMvMatrix();
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
