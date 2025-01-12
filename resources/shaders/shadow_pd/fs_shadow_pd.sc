$input v_position

#include "bgfx_shader.sh"
#include "shaderlib.sh"

uniform vec4 u_depthScaleOffset;  // for GL, map depth values into [0, 1] range
#define u_depthScale u_depthScaleOffset.x
#define u_depthOffset u_depthScaleOffset.y

void main()
{
	float depth = v_position.z/v_position.w * u_depthScale + u_depthOffset;
	gl_FragColor = packFloatToRgba(depth);
}