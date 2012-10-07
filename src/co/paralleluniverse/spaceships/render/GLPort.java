/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships.render;

import static co.paralleluniverse.spacebase.AABB.X;
import static co.paralleluniverse.spacebase.AABB.Y;
import co.paralleluniverse.spacebase.MutableAABB;
import co.paralleluniverse.spacebase.SpaceBase;
import co.paralleluniverse.spacebase.SpatialQueries;
import co.paralleluniverse.spacebase.SpatialSetVisitor;
import co.paralleluniverse.spaceships.Spaceship;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
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
import javax.media.opengl.TraceGL3;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

/**
 *
 * @author pron
 */
public class GLPort implements GLEventListener, KeyListener {

    private static final float KEY_PRESS_TRANSLATE = 10.0f;
    private final int maxItems;
    private final SpaceBase<Spaceship> sb;
    private final MutableAABB port = MutableAABB.create(2);
    private ProgramState shaderState;
    private VAO vao;
    private VBO vertices;
    private PMVMatrix pmv = new PMVMatrix();
    private float x = 1.0f;

    static {
        GLProfile.initSingleton();
    }

    public GLPort(int maxItems, SpaceBase<Spaceship> sb) {
        this.maxItems = maxItems;
        this.sb = sb;

        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilitiesImmutable glcaps = (GLCapabilitiesImmutable) new GLCapabilities(glp);

        GLWindow window = GLWindow.create(glcaps);
        window.setSize(300, 300);
        window.setVisible(true);
        window.setTitle("Spaceships");

        final FPSAnimator animator = new FPSAnimator(window, 60);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(WindowEvent arg0) {
                animator.stop();
                System.exit(0);
            }

        });

        window.addGLEventListener(this);
        window.addKeyListener(this);

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


        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glScalef(1.0f / (float) drawable.getWidth(), 1.0f / (float) drawable.getHeight(), 1);
        pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmv.glLoadIdentity();

        this.vao = shaderState.createVAO(gl);
        vao.bind(gl);

        this.vertices = new VBO(gl, 2, gl.GL_FLOAT, false, maxItems, gl.GL_STATIC_DRAW);
        vertices.write(gl);

        vao.setVertex(gl, "in_Position", vertices);

        shaderState.setUniform(gl, "in_Matrix", 4, 4, pmv.glGetMvMatrixf());
        //shaderState.createUBO(gl, "MatrixBlock");
        //shaderState.getUBO("MatrixBlock").bind(gl).set(gl, "PMatrix", 4, 4, pmv.glGetMvMatrixf());

        shaderState.unbind(gl);
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
            final FloatBuffer verticeb = (FloatBuffer) vertices.getBuffer();
            //System.out.println("XXX " + verticeb + " " + port);
            sb.query(SpatialQueries.contained(port), new SpatialSetVisitor<Spaceship>() {
                @Override
                public void visit(Set<Spaceship> result) {
                    //System.out.println("Seeing " + result.size());
                    for (Spaceship s : result) {
                        verticeb.put((float) s.getX());
                        verticeb.put((float) s.getY());
                    }
                }

            }).join();

            vertices.rewind();

            int numElems = verticeb.limit() / 2;
            vertices.write(gl, 0, numElems);

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
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glScalef((float) drawable.getWidth() / (float) width, (float) drawable.getHeight() / (float) height, 1);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        switch (keyCode) {
            case KeyEvent.VK_UP:
                pmv.glTranslatef(0f, -KEY_PRESS_TRANSLATE, 0f);
                port.min(Y, port.min(Y) + KEY_PRESS_TRANSLATE);
                break;
            case KeyEvent.VK_DOWN:
                pmv.glTranslatef(0f, KEY_PRESS_TRANSLATE, 0f);
                port.min(Y, port.min(Y) - KEY_PRESS_TRANSLATE);
                break;
            case KeyEvent.VK_LEFT:
                pmv.glTranslatef(KEY_PRESS_TRANSLATE, 0f, 0f);
                port.min(X, port.min(X) - KEY_PRESS_TRANSLATE);
                break;
            case KeyEvent.VK_RIGHT:
                pmv.glTranslatef(-KEY_PRESS_TRANSLATE, 0f, 0f);
                port.min(X, port.min(X) + KEY_PRESS_TRANSLATE);
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void print(FloatBuffer buffer) {
        int pos = buffer.position();
        System.err.print(buffer.remaining() + ": ");
        while (buffer.position() < buffer.limit())
            System.err.print(buffer.get() + ", ");
        System.err.println();
        buffer.position(pos);
    }

}
