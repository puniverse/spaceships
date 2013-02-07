/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.spaceships.render;

import com.jogamp.common.nio.Buffers;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLES2;

/**
 *
 * @author pron
 */
public class BO {
    public static int sizeOfGLType(int glType) {
        switch (glType) { // 29
            // case GL2.GL_BITMAP:
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_BYTE:
            case GL2GL3.GL_UNSIGNED_BYTE_3_3_2:
            case GL2GL3.GL_UNSIGNED_BYTE_2_3_3_REV:
                return Buffers.SIZEOF_BYTE;
                
            case GL.GL_SHORT:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_UNSIGNED_SHORT_5_6_5:
            case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
            case GL.GL_HALF_FLOAT:
            case GLES2.GL_HALF_FLOAT_OES:
                return Buffers.SIZEOF_SHORT;
                                
            case GL.GL_FIXED:
            case GL2ES2.GL_INT:
            case GL.GL_UNSIGNED_INT:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL2GL3.GL_UNSIGNED_INT_10_10_10_2:
            case GL2GL3.GL_UNSIGNED_INT_2_10_10_10_REV:                
            case GL2GL3.GL_UNSIGNED_INT_24_8:
            case GL2GL3.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL2GL3.GL_UNSIGNED_INT_5_9_9_9_REV:
            case GL2.GL_HILO16_NV:
            case GL2.GL_SIGNED_HILO16_NV:
                return Buffers.SIZEOF_INT;
                
            case GL2GL3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:
                return Buffers.SIZEOF_LONG;
                
            case GL.GL_FLOAT:
                return Buffers.SIZEOF_FLOAT;
                
            case GL2GL3.GL_DOUBLE:
                return Buffers.SIZEOF_DOUBLE;
                
            case GL2GL3.GL_FLOAT_VEC2:
                return 2 * Buffers.SIZEOF_FLOAT;
            case GL2GL3.GL_FLOAT_VEC3:
                return 3 * Buffers.SIZEOF_FLOAT;
            case GL2GL3.GL_FLOAT_VEC4:
                return 4 * Buffers.SIZEOF_FLOAT;
        
            case GL2GL3.GL_INT_VEC2:
            case GL2GL3.GL_UNSIGNED_INT_VEC2:
                return 2 * Buffers.SIZEOF_INT;
            case GL2GL3.GL_INT_VEC3:
            case GL2GL3.GL_UNSIGNED_INT_VEC3:
                return 3 * Buffers.SIZEOF_INT;
            case GL2GL3.GL_INT_VEC4:
            case GL2GL3.GL_UNSIGNED_INT_VEC4:
                return 4 * Buffers.SIZEOF_INT;
                
            case GL2GL3.GL_FLOAT_MAT2:
                return 2 * 2 * Buffers.SIZEOF_FLOAT;
            case GL2GL3.GL_FLOAT_MAT3:
                return 3 * 3 * Buffers.SIZEOF_FLOAT;
            case GL2GL3.GL_FLOAT_MAT4:
                return 4 * 4 * Buffers.SIZEOF_FLOAT;
            case GL2GL3.GL_FLOAT_MAT2x3:
            case GL2GL3.GL_FLOAT_MAT3x2:
                return 2 * 3 * Buffers.SIZEOF_FLOAT;
            case GL2GL3.GL_FLOAT_MAT2x4:
            case GL2GL3.GL_FLOAT_MAT4x2:
                return 2 * 4 * Buffers.SIZEOF_FLOAT;
            case GL2GL3.GL_FLOAT_MAT3x4:
            case GL2GL3.GL_FLOAT_MAT4x3:
                return 3 * 4 * Buffers.SIZEOF_FLOAT;
        }
        return -1;
    }
}
