#pragma once

#define STB_TRUETYPE_IMPLEMENTATION
#include <stb/stb_truetype.h>
#include "nanovg/nanovg.h"

#include <bgfx/bgfx.h>
#include <yoga/Yoga.h>
#include <yoga/YGLayout.h>

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

        createFonts(vg);

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

    struct Style
    {
        float width;
        float height;
        float x;
        float y;
        YGDirection flexDirection;
    };

    struct Layout
    {
        float width;
        float height;
        float x;
        float y;
    };

    // Layout base node
    class LNode
    {
    private:
        std::vector<LNode> m_children;
        LNode *m_parent;

    public:
        Style m_style;
        YGNodeRef m_layoutNode;

        void draw()
        {
            for (LNode child : m_children)
            {
                child.draw();
            }
        };

        void addChild(LNode child)
        {
            int idx = m_children.size();
            child.m_parent = this;
            m_children.push_back(child);
            YGNodeInsertChild(m_layoutNode, child.m_layoutNode, idx);
        };

        Layout getLayaout()
        {
            Layout layout;

            // YGLayout l = m_layoutNode->getLayout();

            return layout;
        };
    };

    // Layout root node
    class LRoot : public LNode
    {
    public:
        void layout()
        {
            YGNodeCalculateLayout(m_layoutNode, m_style.width, m_style.height, m_style.flexDirection);
        };
    };

}