#version 330 core

in vec2 vTexCoord;
uniform sampler2D myTexture;
out vec4 out_Color;

void main()
{
    out_Color = texture(myTexture,vTexCoord);
}