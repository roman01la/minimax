$input v_normal, v_view, v_texcoord0, v_shadowcoord, v_model, v_normal0

#include "bgfx_shader.sh"
#include "shaderlib.sh"

SAMPLER2DSHADOW(s_shadowMap, 4);
SAMPLER2D(s_texDiffuse, 1);

uniform vec4 u_color;
uniform vec4 u_light_pos;

#include "shadow.sh"

vec2 lit(vec3 _lightDir, vec3 _normal, vec3 _viewDir, float _exp)
{
    // diff
    float ndotl = dot(_normal, _lightDir);

    // spec
    vec3 reflected = 2.0 * ndotl * _normal - _lightDir;
    float rdotv = dot(reflected, _viewDir);
    float spec = step(0.0, ndotl) * pow(max(0.0, rdotv), _exp) * (2.0 + _exp) / 8.0;

    return max(vec2(ndotl, spec), 0.0);
}

vec2 phong(vec3 view, vec3 normal, vec3 light_pos)
{
    vec3 v = view;
    vec3 vd = -normalize(v);
    vec3 n = normal;
    vec3 l = light_pos.xyz;
    vec3 ld = -normalize(l);
    vec2 lc = lit(ld, n, vd, 1.0);

    return lc;
}

void main()
{
    vec2 lighting = phong(v_view, v_normal, u_light_pos);// phong shading
    vec3 color = texture2D(s_texDiffuse, v_texcoord0).xyz * u_color.xyz;// diffuse
    float shadow_intensity = shadow(v_shadowcoord, v_view, v_normal, u_light_pos.xyz, s_shadowMap, 800.0, 0.001);// shadow
    vec3 ambient = 0.6 * color;// ambient
    vec3 brdf = (lighting.x * color + pow(lighting.y, 128.0)) * shadow_intensity;// combined
    vec3 finalColor = toGamma(abs(ambient + brdf));// final

    gl_FragData[0] = vec4(finalColor, 1.0); // diffuse map
    gl_FragData[1] = vec4(v_model, 1.0); // position map
    gl_FragData[2] = v_normal0; // normal map
}
