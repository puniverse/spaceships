#version 330 core

in vec4 pass_Color;
out vec4 out_Color;

void main()
{
    out_Color = pass_Color; // vec4(1.0, 0.0, 0.0, 1.0);
}