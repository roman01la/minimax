#pragma once

#include <bgfx/bgfx.h>

#include "render_pass.h"
#include "program.h"

struct PosTexCoord0Vertex
{
    float m_x;
    float m_y;
    float m_z;
    float m_u;
    float m_v;
};

void screenSpaceQuad(bgfx::VertexLayout layout, float _textureWidth, float _textureHeight, float _texelHalf, bool _originBottomLeft)
{
    float width = 1.0f;
    float height = 1.0f;

    if (3 == bgfx::getAvailTransientVertexBuffer(3, layout))
    {
        bgfx::TransientVertexBuffer vb;
        bgfx::allocTransientVertexBuffer(&vb, 3, layout);
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

class RenderPassCombine : public RenderPass
{
private:
    float m_texelHalf;
    float projMtx[16];
    const bgfx::Caps *caps;
    bgfx::VertexLayout m_layout;
    bgfx::UniformHandle m_texUI;
    bgfx::UniformHandle m_texScreen;
    bgfx::UniformHandle m_texPosition;
    bgfx::UniformHandle m_texNormal;
    bgfx::ProgramHandle m_program;

public:
    bgfx::ViewId m_viewId = 2;

    void init(float width, float height)
    {
        caps = bgfx::getCaps();

        const bgfx::RendererType::Enum renderer = bgfx::getRendererType();
        m_texelHalf = bgfx::RendererType::Direct3D9 == renderer ? 0.5f : 0.0f;

        m_texUI = bgfx::createUniform("s_texUI", bgfx::UniformType::Sampler);
        m_texScreen = bgfx::createUniform("s_texScreen", bgfx::UniformType::Sampler);
        m_texPosition = bgfx::createUniform("s_texPosition", bgfx::UniformType::Sampler);
        m_texNormal = bgfx::createUniform("s_texNormal", bgfx::UniformType::Sampler);

        m_program = loadProgram(
            "/Users/romanliutikov/git/minimax/minimax_c/resources/shaders_out/vs_combine.bin",
            "/Users/romanliutikov/git/minimax/minimax_c/resources/shaders_out/fs_combine.bin");

        m_layout
            .begin()
            .add(bgfx::Attrib::Position, 3, bgfx::AttribType::Float)
            .add(bgfx::Attrib::TexCoord0, 2, bgfx::AttribType::Float)
            .end();

        bx::mtxOrtho(projMtx, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 100.0f, 0.0f, caps->homogeneousDepth);

        bgfx::setViewClear(m_viewId, BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH, 0x000000ff);

        m_isValid = true;
    };

    void resize(float width, float height){};

    void render(float width, float height,
                bgfx::TextureHandle uiTexture,
                bgfx::TextureHandle screenTexture,
                bgfx::TextureHandle positionTexture,
                bgfx::TextureHandle normalTexture)
    {
        bgfx::setState(0 | BGFX_STATE_WRITE_RGB | BGFX_STATE_WRITE_A);
        bgfx::setTexture(0, m_texUI, uiTexture);
        bgfx::setTexture(1, m_texScreen, screenTexture);
        bgfx::setTexture(2, m_texPosition, positionTexture);
        bgfx::setTexture(3, m_texNormal, normalTexture);
        screenSpaceQuad(m_layout, width, height, m_texelHalf, caps->originBottomLeft);

        bgfx::setViewRect(m_viewId, 0, 0, width, height);
        bgfx::setViewTransform(m_viewId, NULL, projMtx);

        bgfx::submit(m_viewId, m_program);
    };

    ~RenderPassCombine()
    {
        bgfx::destroy(m_texUI);
        bgfx::destroy(m_texScreen);
        bgfx::destroy(m_texPosition);
        bgfx::destroy(m_texNormal);
        bgfx::destroy(m_program);
    };
};