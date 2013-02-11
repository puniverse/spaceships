#version 150
 
layout(points) in;
layout (triangle_strip, max_vertices=7) out;
in vec4 pass_Color[1];
in float heading[1];
in float shootLength[1];
out vec4 pass_oColor;
uniform mat4 in_Matrix;

  
 void main()
{
  float size = 16;  
  mat4 RotationMatrix = mat4( cos( heading[0] ), -sin( heading[0] ), 0.0, 0.0,
			    sin( heading[0] ),  cos( heading[0] ), 0.0, 0.0,
			             0.0,           0.0, 1.0, 0.0,
				     0.0,           0.0, 0.0, 1.0 );
  for(int i = 0; i < gl_in.length(); i++)
  {
     // copy attributes
    gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(0,size,0,0));
//    pass_oColor = pass_Color[0];
    pass_oColor = vec4(0,1.0,0,1.0);
 
    // done with the vertex
    EmitVertex();
    gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix *vec4(-size/2,-size,0,0));
    pass_oColor = pass_Color[0];
 
    // done with the vertex
    EmitVertex();
    gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix *vec4(size/2,-size,0,0));
    pass_oColor = pass_Color[0];
 
    // done with the vertex
    EmitVertex();
    EndPrimitive();
    
    if (shootLength[0]>0) {

    ////////
        pass_oColor = vec4(1.0,1.0,1.0,1.0);

        gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix *vec4(-size/10,size,0,0));
    //    pass_oColor = pass_Color[0];
        // done with the vertex
        EmitVertex();


        gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(-size/10,shootLength[0],0,0));
    //    pass_oColor = pass_Color[0];

        // done with the vertex
        EmitVertex();
        gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix *vec4(size/10,size,0,0));
    //    pass_oColor = pass_Color[0];

        // done with the vertex
        EmitVertex();

        gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(size/10,shootLength[0],0,0));
    //    pass_oColor = pass_Color[0];

        // done with the vertex
        EmitVertex();
        EndPrimitive();
    }
  }
    
}