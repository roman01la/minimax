#define Sampler sampler2DShadow

const vec2 poissonDisk[16] = {
vec2(-0.94201624, -0.39906216),
vec2(0.94558609, -0.76890725),
vec2(-0.094184101, -0.92938870),
vec2(0.34495938, 0.29387760),
vec2(-0.91588581, 0.45771432),
vec2(-0.81544232, -0.87912464),
vec2(-0.38277543, 0.27676845),
vec2(0.97484398, 0.75648379),
vec2(0.44323325, -0.97511554),
vec2(0.53742981, -0.47373420),
vec2(-0.26496911, -0.41893023),
vec2(0.79197514, 0.19090188),
vec2(-0.24188840, 0.99706507),
vec2(-0.81409955, 0.91437590),
vec2(0.19984126, 0.78641367),
vec2(0.14383161, -0.14100790)
};

float pcfShadow(vec3 view, Sampler shadow_map, vec3 texCoord, float spread, float bias)
{
    float visibility = 1.0;
    for (int idx = 0; idx < 16; idx++) {
        int index = int(16 * random(floor(view * 1000.0), idx)) % 16;
        float depth = shadow2D(shadow_map, vec3(texCoord.xy + (poissonDisk[index] / spread), texCoord.z - bias));
        visibility -= 0.05 * (1.0 - depth);
    }
    return visibility;
}

float shadow(vec4 shadowcoord, vec3 view, vec3 normal, vec3 light_pos, Sampler shadow_map, float spread, float _bias)
{
    // variable bias
    float cosTheta = clamp(dot(normal, normalize(light_pos)),0, 1);
    float bias = clamp(_bias * tan(acos(cosTheta)), 0, 0.002);
    vec3 texCoord = shadowcoord.xyz / shadowcoord.w;

    // clear depth outside of shadow map boundaries
    bool outside = any(greaterThan(texCoord.xy, vec2_splat(0.99))) || any(lessThan(texCoord.xy, vec2_splat(0.01)));

    return outside ? 1.0 : pcfShadow(view, shadow_map, texCoord, spread, bias);
}
