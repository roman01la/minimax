$input v_texcoord0

#include "bgfx_shader.sh"
#include "shaderlib.sh"

SAMPLER2D(s_texUI, 0);
SAMPLER2D(s_texScreen, 1);
SAMPLER2D(s_texPosition, 2);
SAMPLER2D(s_texNormal, 3);

const vec4 mask_color = vec4(1.0, 0, 0, 1.0);

void main()
{

    vec4 ui_color = texture2D(s_texUI, v_texcoord0);
    vec4 color = texture2D(s_texScreen, v_texcoord0);
//    gl_FragColor = all(equal(ui_color, mask_color)) ? color : ui_color;
    gl_FragColor = color;
}
