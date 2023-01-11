$input v_texcoord0

#include "bgfx_shader.sh"
#include "shaderlib.sh"

SAMPLER2D(s_texUI, 0);
SAMPLER2D(s_texScreen, 1);
SAMPLER2D(s_texPosition, 2);
SAMPLER2D(s_texNormal, 3);

void main()
{

    vec4 ui_color = texture2D(s_texUI, v_texcoord0);
    vec4 color = texture2D(s_texScreen, v_texcoord0);

    // blend UI texture onto screen texture using ALPHA as factor
    gl_FragColor = ui_color * ui_color.a + color * (1.0 - ui_color.a);
}
