/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships.render;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
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

/**
 *
 * @author pron
 */
public class GLPort implements GLEventListener {

    private ShaderProgram shaderProgram;
    private ShaderState shaderState;
    private int vao;

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

        animator.start();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        drawable.setGL(new TraceGL3(new DebugGL3(drawable.getGL().getGL3()), System.err));

        final GL3 gl = drawable.getGL().getGL3();

        gl.glViewport(0, 0, 300, 300);

        ShaderCode vertexShader = ShaderCode.create(gl, gl.GL_VERTEX_SHADER, this.getClass(), "shader", null, "vertex", false);
        ShaderCode fragmentShader = ShaderCode.create(gl, gl.GL_FRAGMENT_SHADER, this.getClass(), "shader", null, "fragment", false);

        if (!vertexShader.compile(gl, System.err))
            throw new GLException("Couldn't compile shader: " + vertexShader);
        if (!fragmentShader.compile(gl, System.err))
            throw new GLException("Couldn't compile shader: " + fragmentShader);

        this.shaderProgram = new ShaderProgram();
        shaderProgram.add(gl, vertexShader, System.err);
        shaderProgram.add(gl, fragmentShader, System.err);
        if (!shaderProgram.link(gl, System.err))
            throw new GLException("Couldn't link program: " + shaderProgram);

        this.shaderState = new ShaderState();
        shaderState.attachShaderProgram(gl, shaderProgram, true);
        shaderState.useProgram(gl, true);

        int[] vao1 = new int[1];
        gl.glGenVertexArrays(1, vao1, 0);
        this.vao = vao1[0];
        gl.glBindVertexArray(vao);

        GLArrayDataServer vertices = GLArrayDataServer.createGLSL("in_Position", 3, gl.GL_FLOAT, false, 3, gl.GL_STATIC_DRAW);
        {
            FloatBuffer verticeb = (FloatBuffer) vertices.getBuffer();
            verticeb.put(-1.0f);
            verticeb.put(-1.0f);
            verticeb.put(0);
            verticeb.put(1.0f);
            verticeb.put(-1.0f);
            verticeb.put(0);
            verticeb.put(0);
            verticeb.put(1.0f);
            verticeb.put(0);
        }
        vertices.seal(gl, true);

        GLArrayDataServer colors = GLArrayDataServer.createGLSL("in_Color",  4, gl.GL_FLOAT, false, 3, gl.GL_STATIC_DRAW);
        colors.setName("in_Color");
        {
            FloatBuffer colorb = (FloatBuffer)colors.getBuffer();
            colorb.put( 1);    colorb.put( 0);     colorb.put( 0);    colorb.put( 1);
            colorb.put( 0);    colorb.put( 1);     colorb.put( 0);    colorb.put( 1);
            colorb.put( 0);    colorb.put( 0);     colorb.put( 1);    colorb.put( 1);
        }
        colors.seal(gl, true);

        gl.glBindVertexArray(0);
        gl.glClearColor(0, 0, 0, 1);

        shaderState.useProgram(gl, false);
        System.err.println(Thread.currentThread() + " " + shaderState);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();
        shaderState.destroy(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();

        shaderState.useProgram(gl, true);
        gl.glBindVertexArray(vao);

        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glDrawArrays(gl.GL_TRIANGLES, 0, 3);

        gl.glBindVertexArray(0);
        shaderState.useProgram(gl, false);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glViewport(0, 0, width, height);
    }

}
