#version 150
 
layout(points) in;
layout (triangle_strip, max_vertices=10) out;
in float pass_Color[1];
in float heading[1];
in float shootLength[1];
uniform mat4 in_Matrix;
out float explosionTime;
out vec2 vTexCoord;
out float light;


  
 void main()
{
  vec4 lightSource = vec4(0.707,0,0.707,0);
  explosionTime = pass_Color[0];
  float size = 16;  
  mat4 RotationMatrix = mat4( cos( heading[0] ), -sin( heading[0] ), 0.0, 0.0,
			    sin( heading[0] ),  cos( heading[0] ), 0.0, 0.0,
			             0.0,           0.0, 1.0, 0.0,
				     0.0,           0.0, 0.0, 1.0 );
  for(int i = 0; i < gl_in.length(); i++)
  {
    gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(-size,-size,0,0));
    vTexCoord = vec2(0.0,1.0);
    light = dot(RotationMatrix * vec4(-1,0,0,0),lightSource);
    EmitVertex();


    gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(-size,size,0,0));
    vTexCoord = vec2(0.0,0.0);
    light = dot(RotationMatrix * vec4(-1,0,0,0),lightSource);
    EmitVertex();

    gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(0,-size,0,0));
    vTexCoord = vec2(0.5,1.0);
    light = dot(RotationMatrix * vec4(0,0,1,0),lightSource);
    EmitVertex();

    gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(0,size,0,0));
    vTexCoord = vec2(0.5,0.0);
    light = dot(RotationMatrix * vec4(0,0,1,0),lightSource);
    EmitVertex();

    gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(size,-size,0,0));
    vTexCoord = vec2(1.0,1.0);
    light = dot(RotationMatrix * vec4(1,0,0,0),lightSource);
    EmitVertex();

    gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(size,size,0,0));
    vTexCoord = vec2(1.0,0.0);
    light = dot(RotationMatrix * vec4(1,0,0,0),lightSource);
    EmitVertex();

    EndPrimitive();
    
    if (shootLength[0]>0) {
        light = 1.0;

        gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix *vec4(-size/10,size,0,0));
        vTexCoord = vec2(0.5,0.8);
        EmitVertex();

        gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(-size/10,shootLength[0],0,0));
        vTexCoord = vec2(0.5,0.9);
        EmitVertex();

        gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix *vec4(size/10,size,0,0));
        vTexCoord = vec2(0.5,0.8);
        EmitVertex();

        gl_Position = in_Matrix * (gl_in[i].gl_Position + RotationMatrix * vec4(size/10,shootLength[0],0,0));
        vTexCoord = vec2(0.5,0.9);
        EmitVertex();
        EndPrimitive();
    }
  }
    
}