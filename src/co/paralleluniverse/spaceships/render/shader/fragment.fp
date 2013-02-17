#version 330 core

in vec2 vTexCoord;
uniform sampler2D spaceshipTex;
uniform sampler2D explosionTex;
out vec4 out_Color;
in float tex;


void main()
{
    if (tex>0) {
        out_Color = texture(explosionTex,vTexCoord) * (1-tex);
//        out_Color[3] = tex;
    } else
        out_Color = texture(spaceshipTex,vTexCoord);
}