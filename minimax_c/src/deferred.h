#pragma once

#include <bgfx/bgfx.h>

namespace minimax::deferred
{
    bgfx::ViewId s_viewId = 1;

    struct PosTexCoord0Vertex
    {
        float m_x;
        float m_y;
        float m_z;
        float m_u;
        float m_v;

        static void init()
        {
            ms_layout
                .begin()
                .add(bgfx::Attrib::Position, 3, bgfx::AttribType::Float)
                .add(bgfx::Attrib::TexCoord0, 2, bgfx::AttribType::Float)
                .end();
        }

        static bgfx::VertexLayout ms_layout;
    };

    bgfx::VertexLayout PosTexCoord0Vertex::ms_layout;

    void screenSpaceQuad(float _textureWidth, float _textureHeight, float _texelHalf, bool _originBottomLeft)
    {
        float width = 1.0f;
        float height = 1.0f;

        if (3 == bgfx::getAvailTransientVertexBuffer(3, PosTexCoord0Vertex::ms_layout))
        {
            bgfx::TransientVertexBuffer vb;
            bgfx::allocTransientVertexBuffer(&vb, 3, PosTexCoord0Vertex::ms_layout);
            PosTexCoord0Vertex *vertex = (PosTexCoord0Vertex *)vb.data;

            const float minx = -width;
            const float maxx = width;
            const float miny = 0.0f;
            const float maxy = height * 2.0f;

            const float texelHalfW = _texelHalf / _textureWidth;
            const float texelHalfH = _texelHalf / _textureHeight;
            const float minu = -1.0f + texelHalfW;
            const float maxu = 1.0f + texelHalfH;

            const float zz = 0.0f;

            float minv = texelHalfH;
            float maxv = 2.0f + texelHalfH;

            if (_originBottomLeft)
            {
                float temp = minv;
                minv = maxv;
                maxv = temp;

                minv -= 1.0f;
                maxv -= 1.0f;
            }

            vertex[0].m_x = minx;
            vertex[0].m_y = miny;
            vertex[0].m_z = zz;
            vertex[0].m_u = minu;
            vertex[0].m_v = minv;

            vertex[1].m_x = maxx;
            vertex[1].m_y = miny;
            vertex[1].m_z = zz;
            vertex[1].m_u = maxu;
            vertex[1].m_v = minv;

            vertex[2].m_x = maxx;
            vertex[2].m_y = maxy;
            vertex[2].m_z = zz;
            vertex[2].m_u = maxu;
            vertex[2].m_v = maxv;

            bgfx::setVertexBuffer(0, &vb);
        }
    };

    static float s_texelHalf = 0.0f;
    const bgfx::Caps *caps;
    bgfx::FrameBufferHandle s_uiFb;
    bgfx::TextureHandle s_uiTexture;
    bgfx::UniformHandle s_texUI;
    bgfx::UniformHandle s_texScreen;
    bgfx::ProgramHandle s_program;

    void createUIFrameBuffer(float width, float height)
    {
        bgfx::TextureHandle textures[2] = {
            bgfx::createTexture2D(width, height, false, 1, bgfx::TextureFormat::RGBA8, BGFX_TEXTURE_RT),
            bgfx::createTexture2D(width, height, false, 1, bgfx::TextureFormat::D24S8, BGFX_TEXTURE_RT | BGFX_TEXTURE_RT_WRITE_ONLY)};

        s_uiTexture = textures[0];
        s_uiFb = bgfx::createFrameBuffer(2, textures, true);
    };

    void setup(float width, float height)
    {
        caps = bgfx::getCaps();

        const bgfx::RendererType::Enum renderer = bgfx::getRendererType();
        s_texelHalf = bgfx::RendererType::Direct3D9 == renderer ? 0.5f : 0.0f;

        s_texUI = bgfx::createUniform("s_texUI", bgfx::UniformType::Sampler);
        s_texScreen = bgfx::createUniform("s_texScreen", bgfx::UniformType::Sampler);

        PosTexCoord0Vertex::init();

        createUIFrameBuffer(width, height);
    };

    void render(float width, float height)
    {
        float proj[16];
        bx::mtxOrtho(proj, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 100.0f, 0.0f, caps->homogeneousDepth);

        bgfx::setState(0 | BGFX_STATE_WRITE_RGB | BGFX_STATE_WRITE_A);
        bgfx::setTexture(0, s_texUI, s_uiTexture);
        // bgfx::setTexture(1, s_texScreen, m_lightBufferTex);
        screenSpaceQuad(width, height, s_texelHalf, caps->originBottomLeft);

        bgfx::setViewRect(s_viewId, 0, 0, width, height);
        bgfx::setViewTransform(s_viewId, NULL, proj);
        bgfx::setViewFrameBuffer(s_viewId, s_uiFb);

        bgfx::submit(s_viewId, s_program);
    };

    void onResize(float width, float height)
    {
        bgfx::destroy(s_uiFb);
        createUIFrameBuffer(width, height);
    };

    void destroy()
    {
        bgfx::destroy(s_uiFb);
        bgfx::destroy(s_texUI);
        bgfx::destroy(s_texScreen);
        bgfx::destroy(s_program);
    };
}