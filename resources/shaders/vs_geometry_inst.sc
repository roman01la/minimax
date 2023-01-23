$input a_position, i_data0, i_data1, i_data2, i_data3

#include "bgfx_shader.sh"
#include "shaderlib.sh"

void main()
{
	mat4 model = mtxFromCols(i_data0, i_data1, i_data2, i_data3);
	vec4 worldPos = mul(model, vec4(a_position, 1.0));

	gl_Position = mul(u_viewProj, worldPos);
}

