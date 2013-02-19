#version 330 core

in vec2 vTexCoord;
uniform sampler2D spaceshipTex;
uniform sampler2D explosionTex;
out vec4 out_Color;
in float tex;
in float light;


void main()
{   
    float effectiveLight;
    if (tex>0) {
        out_Color = (texture(explosionTex,vTexCoord) * texture(spaceshipTex,vTexCoord) * 1)  * (1-tex) ;
    } else {
        if (light< 0.2) effectiveLight=0.2;
        else effectiveLight = light;
        out_Color = texture(spaceshipTex,vTexCoord) * effectiveLight * 1.4;
    }
}