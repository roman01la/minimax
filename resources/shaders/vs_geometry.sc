$input a_position, a_normal, a_texcoord0
$output v_normal, v_view, v_texcoord0, v_shadowcoord, v_model, v_normal0

#include "bgfx_shader.sh"
#include "shaderlib.sh"

uniform mat4 u_light_mtx;

void main()
{
    gl_Position = mul(u_modelViewProj, vec4(a_position, 1.0));
    v_normal = normalize(mul(u_modelView, vec4(a_normal.xyz, 0.0) ).xyz);
    v_view = mul(u_modelView, vec4(a_position, 1.0)).xyz;

    v_model = mul(u_modelView, vec4(a_position, 1.0)).xyz;
    v_normal0 = normalize(a_normal);

    v_shadowcoord = mul(u_light_mtx, vec4(a_position, 1.0));

    v_texcoord0 = a_texcoord0;
}
