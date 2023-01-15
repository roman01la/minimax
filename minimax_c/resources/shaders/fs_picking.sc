#include "bgfx_shader.sh"
#include "shaderlib.sh"

uniform vec4 u_id;

void main()
{
    gl_FragColor = vec4(u_id.xyz, 1.0);
}
