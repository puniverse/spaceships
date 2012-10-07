/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships.render;

import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.media.opengl.GL3;
import javax.media.opengl.GLException;

/**
 *
 * @author pron
 */
public class ProgramState {

    private final ShaderProgram shader;
    private final int vao;
    private final Map<String, Integer> attributes = Collections.synchronizedMap(new HashMap<String, Integer>());
    private final BitSet vertexAttributes = new BitSet();

    public ProgramState(GL3 gl, ShaderProgram shader) {
        this.shader = shader;

        int[] vao1 = new int[1];
        gl.glGenVertexArrays(1, vao1, 0);
        this.vao = vao1[0];
    }

    public void bind(GL3 gl) {
        gl.glBindVertexArray(vao);
        shader.useProgram(gl, true);
    }
    
    public void unbind(GL3 gl) {
        shader.useProgram(gl, false);
        gl.glBindVertexArray(0);
    }
    
    private int getLocation(GL3 gl, String attributeName) {
        Integer index = attributes.get(attributeName);
        if(index == null) {
            index = gl.glGetAttribLocation(shader.program(), attributeName);
            attributes.put(attributeName, index);
        }
        return index;
    }
    
    public void setUniform(GL3 gl, String attributeName, int value) {
        gl.glUniform1i(getLocation(gl, attributeName), value);
    }
        
    public void setUniform(GL3 gl, String attributeName, int v0, int v1) {
        gl.glUniform2i(getLocation(gl, attributeName), v0, v1);
    }
    
    public void setUniform(GL3 gl, String attributeName, int v0, int v1, int v2) {
        gl.glUniform3i(getLocation(gl, attributeName), v0, v1, v2);
    }
    
    public void setUniform(GL3 gl, String attributeName, int v0, int v1, int v2, int v3) {
        gl.glUniform4i(getLocation(gl, attributeName), v0, v1, v2, v3);
    }
    
    public void setUniform(GL3 gl, String attributeName, float value) {
        gl.glUniform1f(getLocation(gl, attributeName), value);
    }
    
    public void setUniform(GL3 gl, String attributeName, float v0, float v1) {
        gl.glUniform2f(getLocation(gl, attributeName), v0, v1);
    }
    
    public void setUniform(GL3 gl, String attributeName, float v0, float v1, float v2) {
        gl.glUniform3f(getLocation(gl, attributeName), v0, v1, v2);
    }
    
    public void setUniform(GL3 gl, String attributeName, float v0, float v1, float v2, float v3) {
        gl.glUniform4f(getLocation(gl, attributeName), v0, v1, v2, v3);
    }
    
    public void setUniform(GL3 gl, String attributeName, int components, IntBuffer buffer) {
        final int location = getLocation(gl, attributeName);
        final int elements = buffer.remaining()/components;
        switch(components) {
            case 1: gl.glUniform1iv(location, elements, buffer); break;
            case 2: gl.glUniform2iv(location, elements, buffer); break;
            case 3: gl.glUniform3iv(location, elements, buffer); break;
            case 4: gl.glUniform4iv(location, elements, buffer); break;
            default:
                throw new GLException("glUniform vector only available for 1iv 2iv, 3iv and 4iv");
        }
    }

    public void setUniform(GL3 gl, String attributeName, int components, FloatBuffer buffer) {
        final int location = getLocation(gl, attributeName);
        final int elements = buffer.remaining()/components;
        switch(components) {
            case 1: gl.glUniform1fv(location, elements, buffer); break;
            case 2: gl.glUniform2fv(location, elements, buffer); break;
            case 3: gl.glUniform3fv(location, elements, buffer); break;
            case 4: gl.glUniform4fv(location, elements, buffer); break;
            default:
                throw new GLException("glUniform vector only available for 1fv 2fv, 3fv and 4fv");
        }
    }
    
    public void setUniform(GL3 gl, String attributeName, int rows, int columns, FloatBuffer buffer) {
        final int location = getLocation(gl, attributeName);
        final int elements = buffer.remaining()/(rows * columns);
        switch(columns) {
            case 2: gl.glUniformMatrix2fv(location, elements, false, buffer); break;
            case 3: gl.glUniformMatrix3fv(location, elements, false, buffer); break;
            case 4: gl.glUniformMatrix4fv(location, elements, false, buffer); break;
            default:
                  throw new GLException("glUniformMatrix only available for 2fv, 3fv and 4fv");
        }
    }
    
    public void setVertex(GL3 gl, String attributeName, VBO vbo) {
        vbo.bind(gl);
        final int location = getLocation(gl, attributeName);
        gl.glEnableVertexAttribArray(location);
        if(vbo.getBuffer() instanceof FloatBuffer)
            gl.glVertexAttribPointer(location, vbo.getComponents(), vbo.getComponentType(), vbo.isNormalized(), vbo.getStride(), 0);
        else if(vbo.getBuffer() instanceof IntBuffer)
            gl.glVertexAttribIPointer(location, vbo.getComponents(), vbo.getComponentType(), vbo.getStride(), 0);
        if(vbo.getBuffer() instanceof DoubleBuffer)
            gl.glVertexAttribLPointer(location, vbo.getComponents(), vbo.getComponentType(), vbo.getStride(), 0);
        else
            throw new GLException("Unrecognized buffer type: " + vbo.getBuffer().getClass().getName());
        vertexAttributes.set(location);
    }
    
    public void destroy(GL3 gl) {
        for (int i = vertexAttributes.nextSetBit(0); i >= 0; i = vertexAttributes.nextSetBit(i+1))
            gl.glDisableVertexAttribArray(i);
        
        shader.useProgram(gl, false);
        gl.glBindVertexArray(0);
        gl.glDeleteVertexArrays(1, new int[]{vao}, 0);
        
        shader.release(gl, true);
    }
}
