#pragma once

#include <bgfx/bgfx.h>

class RenderPass
{
public:
    bgfx::ViewId m_viewId;
    bool m_isValid;
    void init(float width, float height){};
    void render(float width, float height){};
    void resize(float width, float height){};
    ~RenderPass(){};
};