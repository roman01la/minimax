$input v_texcoord0

#include "bgfx_shader.sh"
#include "shaderlib.sh"

SAMPLER2D(s_texScreen, 0);
SAMPLER2D(s_texPosition, 2);
SAMPLER2D(s_texNormal, 3);

void main()
{

    vec4 color = texture2D(s_texScreen, v_texcoord0);
    gl_FragColor = color;
}
