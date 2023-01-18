#pragma once

#include <bgfx/bgfx.h>

#include "dbg.h"
#include "render_pass.h"

class RenderPassGeometry : public RenderPass
{
private:
    bgfx::FrameBufferHandle m_fb;

    void createFrameBuffer(float width, float height)
    {
        uint64_t flags =
            0 |
            BGFX_TEXTURE_RT_MSAA_X4 |
            BGFX_SAMPLER_MIN_POINT |
            BGFX_SAMPLER_MAG_POINT |
            BGFX_SAMPLER_MIP_POINT |
            BGFX_SAMPLER_U_CLAMP |
            BGFX_SAMPLER_V_CLAMP;

        bgfx::TextureHandle textures[3] = {
            bgfx::createTexture2D(width, height, false, 1, bgfx::TextureFormat::RGBA8, flags),
            bgfx::createTexture2D(width, height, false, 1, bgfx::TextureFormat::RGBA16F, flags),
            bgfx::createTexture2D(width, height, false, 1, bgfx::TextureFormat::RGBA8, flags)};

        m_texColor = textures[0];
        m_texPosition = textures[1];
        m_texNormal = textures[2];
        m_fb = bgfx::createFrameBuffer(3, textures, true);
    };

    void destroyFrameBuffer()
    {
        bgfx::destroy(m_fb);
        bgfx::destroy(m_texColor);
        bgfx::destroy(m_texPosition);
        bgfx::destroy(m_texNormal);
    };

public:
    bgfx::ViewId m_viewId = 5;
    bgfx::TextureHandle m_texColor;
    bgfx::TextureHandle m_texPosition;
    bgfx::TextureHandle m_texNormal;
    void (*renderFn)();

    void init(float width, float height)
    {
        createFrameBuffer(width, height);

        m_isValid = true;
    };

    void render(float width, float height)
    {
        bgfx::setViewRect(m_viewId, 0, 0, width, height);
        bgfx::setViewFrameBuffer(m_viewId, m_fb);

        bgfx::touch(m_viewId);

        renderFn();
    };

    void resize(float width, float height)
    {
        destroyFrameBuffer();
        createFrameBuffer(width, height);
    };

    ~RenderPassGeometry()
    {
        destroyFrameBuffer();
    };
};