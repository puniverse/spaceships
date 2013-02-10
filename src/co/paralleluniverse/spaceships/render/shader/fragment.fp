#version 330 core

in vec4 pass_oColor;
out vec4 out_Color;

void main()
{
    out_Color = pass_oColor; // vec4(1.0, 0.0, 0.0, 1.0);
}