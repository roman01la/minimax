#pragma once

#include <bgfx/bgfx.h>

#include "ui.h"
#include "render_pass.h"

class RenderPassUI : public RenderPass
{
private:
    NVGcontext *vg;
    bgfx::FrameBufferHandle m_fb;

    void createFrameBuffer(float width, float height)
    {
        bgfx::TextureHandle textures[2] = {
            bgfx::createTexture2D(width, height, false, 1, bgfx::TextureFormat::BGRA8, BGFX_TEXTURE_RT),
            bgfx::createTexture2D(width, height, false, 1, bgfx::TextureFormat::D24, BGFX_TEXTURE_RT)};

        m_texture = textures[0];
        m_fb = bgfx::createFrameBuffer(2, textures, true);
    };

public:
    bgfx::ViewId m_viewId = 1;
    bgfx::TextureHandle m_texture;
    void (*renderFn)(NVGcontext *vg);

    void init(float width, float height)
    {
        vg = minimax::ui::init(m_viewId);

        m_isValid = bool(vg);

        createFrameBuffer(width, height);

        bgfx::setViewClear(m_viewId, BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH, 0x00000000);
        bgfx::setViewMode(m_viewId, bgfx::ViewMode::Sequential);
    };

    void render(float width, float height, int dpr)
    {
        bgfx::setViewRect(m_viewId, 0, 0, width, height);
        bgfx::setViewFrameBuffer(m_viewId, m_fb);

        nvgBeginFrame(vg, width / dpr, height / dpr, dpr);
        renderFn(vg);
        nvgEndFrame(vg);

        bgfx::touch(m_viewId);
    };

    void resize(float width, float height)
    {
        bgfx::destroy(m_fb);
        createFrameBuffer(width, height);
    };

    ~RenderPassUI()
    {
        bgfx::destroy(m_fb);
        bgfx::destroy(m_texture);
        nvgDelete(vg);
    };
};