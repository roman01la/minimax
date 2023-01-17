#pragma once

#define STB_TRUETYPE_IMPLEMENTATION
#include <stb/stb_truetype.h>
#include "nanovg/nanovg.h"

#include <bgfx/bgfx.h>
#include <yoga/Yoga.h>

namespace minimax::ui
{

    // animation
    class Spring
    {
    private:
        int stiffness;
        int mass;
        int damping;
        float springVelocity;
        float springLength;
        float springRestLength;

    public:
        Spring(int _stiffness, int _mass, int _damping, float from, float to)
        {
            springVelocity = 0.0f;
            springLength = from;
            springRestLength = to;
            stiffness = -_stiffness;
            damping = -_damping;
            mass = _mass;
        };
        float step(float dt)
        {
            float springForce = stiffness * (springLength - springRestLength);
            float springDamping = damping * springVelocity;
            float springAcceleration = (springForce + springDamping) / mass;

            springVelocity += springAcceleration * dt;
            springLength += springVelocity * dt;

            return floorf(springLength * 100) / 100;
        };
    };
    // /animation

    // fonts
    int32_t createFont(NVGcontext *_ctx, const char *_name, const char *_filePath)
    {
        uint32_t size;
        void *data = load(_filePath, &size);
        if (NULL == data)
        {
            return -1;
        }

        return nvgCreateFontMem(_ctx, _name, (uint8_t *)data, size, 1);
    }

    void createFonts(NVGcontext *vg)
    {
        createFont(vg, "IBMPlexMono Regular", "/Users/romanliutikov/git/minimax/minimax_c/resources/fonts/IBM_Plex_Mono/IBMPlexMono-Regular.ttf");
    };

    // /fonts

    NVGcontext *init()
    {
        NVGcontext *vg = nvgCreate(1, 0);
        bgfx::setViewMode(0, bgfx::ViewMode::Sequential);

        return vg;
    };

    void rect(NVGcontext *vg, float x, float y, float width, float height, NVGcolor color)
    {
        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, height);
        nvgFillColor(vg, color);
        nvgFill(vg);
        nvgClosePath(vg);
    };

}