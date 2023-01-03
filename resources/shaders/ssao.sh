SAMPLER2D(s_ssaoNoise, 1);

uniform vec3 u_ssao_samples[64];

float ssao(vec2 tex_coord)
{
    float noise_size = 4.0;
    float kernelSize = 64.0;
    float radius = 0.4;
    float bias = 0.025;

    vec2 noise_scale = u_viewRect.zw / noise_size;

    vec3 fragPos = texture2D(s_texPosition, tex_coord).xyz;
    vec3 normal = normalize(texture2D(s_texNormal, tex_coord).xyz);
    vec3 randVec = normalize(texture2D(s_ssaoNoise, tex_coord * noise_scale).xyz);

    vec3 tangent = normalize(randVec - normal * dot(randVec, normal));
    vec3 bitangent = cross(normal, tangent);
    mat3 tbn = mat3(tangent, bitangent, normal);

    float occlusion = 0.0;
    for (int idx = 0; idx < kernelSize; ++idx)
    {
        vec3 samplePos = tbn * u_ssao_samples[idx];
        samplePos = samplePos * radius + fragPos;

        vec4 offset = vec4(samplePos, 1.0);
        offset = offset * u_proj;
        offset.xyz /= offset.w;
        offset.xyz = offset.xyz * 0.5 + 0.5;

        float sampleDepth = texture2D(s_texPosition, offset.xy).z;
        float rangeCheck = smoothstep(0.0, 1.0, radius / abs(fragPos.z - sampleDepth));

        occlusion += (sampleDepth >= samplePos.z + bias ? 1.0 : 0.0) * rangeCheck;
    }

    occlusion = 1.0 - (occlusion / kernelSize);

    return occlusion;
}
