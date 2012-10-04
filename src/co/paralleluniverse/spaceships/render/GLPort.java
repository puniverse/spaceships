/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships.render;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

/**
 *
 * @author pron
 */
public class GLPort implements GLEventListener {
    private ShaderProgram shader;
    private ShaderState shaderState;
    private double theta = 0;
    private double s = 0;
    private double c = 0;

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
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        ShaderCode vertexShader = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "shader", null, "vertex", false);
        ShaderCode fragmentShader = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "shader", null, "fragment", false);
        
        this.shader = new ShaderProgram();
        shader.add(gl, vertexShader, System.err);
        shader.add(gl, fragmentShader, System.err);
        
        this.shaderState = new ShaderState();
        shaderState.attachShaderProgram(gl, shader, true);
        
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        shaderState.destroy(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        theta += 0.01;
        s = Math.sin(theta);
        c = Math.cos(theta);

        final GL2 gl = drawable.getGL().getGL2();

        gl.glViewport(0, 0, 300, 300);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        // draw a triangle filling the window
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3f(1, 0, 0);
        gl.glVertex2d(-c, -c);
        gl.glColor3f(0, 1, 0);
        gl.glVertex2d(0, c);
        gl.glColor3f(0, 0, 1);
        gl.glVertex2d(s, -s);
        gl.glEnd();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

}
