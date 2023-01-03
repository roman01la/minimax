// https://github.com/McNopper/OpenGL/blob/master/Example42/shader/fxaa.frag.glsl

const float lumaThreshold = 0.5;
const float mulReduce = 1.0 / 8.0;
const float minReduce = 1.0 / 128.0;
const float maxSpan = 8.0;

vec4 texOffset(sampler2D colorTexture, vec2 texCoord, vec2 offset)
{
  return texture2D(colorTexture, texCoord + offset);
}

vec4 fxaa(sampler2D colorTexture, vec2 texCoord)
{
    vec2 texelStep = u_viewTexel.xy;

    vec3 rgbM = texOffset(colorTexture, texCoord, vec2(0, 0)).rgb;

    vec3 rgbNW = texOffset(colorTexture, texCoord, vec2(-1, 1)).rgb;
    vec3 rgbNE = texOffset(colorTexture, texCoord, vec2(1, 1)).rgb;
    vec3 rgbSW = texOffset(colorTexture, texCoord, vec2(-1, -1)).rgb;
    vec3 rgbSE = texOffset(colorTexture, texCoord, vec2(1, -1)).rgb;

    float lumaNW = luma(rgbNW);
    float lumaNE = luma(rgbNE);
    float lumaSW = luma(rgbSW);
    float lumaSE = luma(rgbSE);
    float lumaM = luma(rgbM);

    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

    // If contrast is lower than a maximum threshold ...
    if (lumaMax - lumaMin <= lumaMax * lumaThreshold)
    {
        // ... do no AA and return.
        return vec4(rgbM, 1.0);
    }

    // Sampling is done along the gradient.
    vec2 samplingDirection = vec2(
    -((lumaNW + lumaNE) - (lumaSW + lumaSE)),
    ((lumaNW + lumaSW) - (lumaNE + lumaSE))
    );

    // Sampling step distance depends on the luma: The brighter the sampled texels, the smaller the final sampling step direction.
    // This results, that brighter areas are less blurred/more sharper than dark areas.
    float samplingDirectionReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * 0.25 * mulReduce, minReduce);

    // Factor for norming the sampling direction plus adding the brightness influence.
    float minSamplingDirectionFactor = 1.0 / (min(abs(samplingDirection.x), abs(samplingDirection.y)) + samplingDirectionReduce);

    // Calculate final sampling direction vector by reducing, clamping to a range and finally adapting to the texture size.
    samplingDirection = clamp(samplingDirection * minSamplingDirectionFactor, vec2(-maxSpan), vec2(maxSpan)) * texelStep;

    // Inner samples on the tab.
    vec3 rgbSampleNeg = texture2D(colorTexture, texCoord + samplingDirection * (1.0/3.0 - 0.5)).rgb;
    vec3 rgbSamplePos = texture2D(colorTexture, texCoord + samplingDirection * (2.0/3.0 - 0.5)).rgb;

    vec3 rgbTwoTab = (rgbSamplePos + rgbSampleNeg) * 0.5;

    // Outer samples on the tab.
    vec3 rgbSampleNegOuter = texture2D(colorTexture, texCoord + samplingDirection * (0.0/3.0 - 0.5)).rgb;
    vec3 rgbSamplePosOuter = texture2D(colorTexture, texCoord + samplingDirection * (3.0/3.0 - 0.5)).rgb;

    vec3 rgbFourTab = (rgbSamplePosOuter + rgbSampleNegOuter) * 0.25 + rgbTwoTab * 0.5;

    // Calculate luma for checking against the minimum and maximum value.
    float lumaFourTab = luma(rgbFourTab);

    // Are outer samples of the tab beyond the edge ...
    if (lumaFourTab < lumaMin || lumaFourTab > lumaMax)
    {
        // ... yes, so use only two samples.
        return vec4(rgbTwoTab, 1.0);
    }
    else
    {
        // ... no, so use four samples.
        return vec4(rgbFourTab, 1.0);
    }
}
