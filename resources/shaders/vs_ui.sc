$input a_position, a_texcoord0, a_color0
$output v_texcoord0, v_color0

#include "bgfx_shader.sh"
#include "shaderlib.sh"

void main()
{
    v_texcoord0 = a_texcoord0;
    v_color0 = a_color0;
    gl_Position = mul(u_viewProj, vec4(a_position.xy, 0.0, 1.0));
}
