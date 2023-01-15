$input a_position

#include "bgfx_shader.sh"
#include "shaderlib.sh"

void main()
{
    gl_Position = mul(u_modelViewProj, vec4(a_position, 1.0));
}
