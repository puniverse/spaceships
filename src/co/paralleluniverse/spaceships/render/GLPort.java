/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships.render;

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

    private ProgramState shaderState;
    private VAO vao;
    private VBO vertices;
    private PMVMatrix pmv = new PMVMatrix();
    private float x = 1.0f;

    static {
        GLProfile.initSingleton();
    }

    public GLPort() {
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
        //drawable.setGL(new DebugGL3(drawable.getGL().getGL3()));
        drawable.setGL(new TraceGL3(new DebugGL3(drawable.getGL().getGL3()), System.err));

        final GL3 gl = drawable.getGL().getGL3();

        gl.glEnable(gl.GL_VERTEX_PROGRAM_POINT_SIZE);
        gl.glViewport(0, 0, 300, 300);
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
        pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmv.glLoadIdentity();
        //pmv.glScalef((float)drawable.getWidth(), (float)drawable.getHeight(), 1);
        pmv.glScalef(0.5f, 0.5f, 1.0f);

        print(pmv.glGetPMatrixf());

        this.vao = shaderState.createVAO(gl);
        vao.bind(gl);

        this.vertices = new VBO(gl, 2, gl.GL_FLOAT, false, 3, gl.GL_STATIC_DRAW);
        {
            FloatBuffer verticeb = (FloatBuffer) vertices.getBuffer();
            verticeb.put(-1.0f);
            verticeb.put(-1.0f);
            verticeb.put(1.0f);
            verticeb.put(-1.0f);
            verticeb.put(0);
            verticeb.put(1.0f);
        }
        vertices.write(gl);

        vao.setVertex(gl, "in_Position", vertices);

        //shaderState.setUniform(gl, "in_Matrix", 4, 4, pmv.glGetPMatrixf());
        shaderState.createUBO(gl, "MatrixBlock");
        shaderState.getUBO("MatrixBlock").bind(gl).set(gl, "PMatrix", 4, 4, pmv.glGetPMatrixf());

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
        final GL3 gl = drawable.getGL().getGL3();

        x -= 0.001f;

        shaderState.bind(gl);
        vao.bind(gl);

        vertices.position(2);
        {
            final FloatBuffer verticeb = (FloatBuffer) vertices.getBuffer();
            verticeb.put(0);
            verticeb.put(x);
        }
        vertices.rewind();
        vertices.write(gl, 2, 1);

        //shaderState.setUniform(gl, "in_Matrix", 4, 4, pmv.glGetPMatrixf());
        //shaderState.getUBO("MatrixBlock").set(gl, "PMatrix", 4, 4, pmv.glGetPMatrixf());

        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glDrawArrays(gl.GL_POINTS, 0, 3);

        vao.unbind(gl);
        shaderState.unbind(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glViewport(0, 0, width, height);
        pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        //pmv.glScalef((float)width, (float)height, 1);
        pmv.glScalef(0.5f, 0.5f, 1.0f);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void keyReleased(KeyEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void keyTyped(KeyEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
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
