#include "bgfx_shader.sh"
#include "shaderlib.sh"

uniform vec4 u_color;

void main()
{
    gl_FragColor = u_color;
}
