#include <stdio.h>
#include <bx/bx.h>
#include <bx/math.h>
#include <bgfx/bgfx.h>
#include <bgfx/platform.h>
#include <GLFW/glfw3.h>

#if BX_PLATFORM_LINUX || BX_PLATFORM_BSD
#define GLFW_EXPOSE_NATIVE_X11
#define GLFW_EXPOSE_NATIVE_GLX
#elif BX_PLATFORM_OSX
#define GLFW_EXPOSE_NATIVE_COCOA
#define GLFW_EXPOSE_NATIVE_NSGL
#elif BX_PLATFORM_WINDOWS
#define GLFW_EXPOSE_NATIVE_WIN32
#define GLFW_EXPOSE_NATIVE_WGL
#endif

#include <GLFW/glfw3native.h>

#include "dbg.h"
#include "allocator.h"
#include "fs.h"
#include "state.h"
#include "glfw_listeners.h"
#include "model.h"
#include "scene.h"
#include "ui.h"
#include "geometry_pass.h"
#include "ui_pass.h"
#include "combine_pass.h"

void error_callback(int _error, const char *_description)
{
    DBG("GLFW error %d: %s", _error, _description);
}

GLFWwindow *init_glfw(State *state)
{
    glfwSetErrorCallback(error_callback);

    if (!glfwInit())
    {
        DBG("glfwInit failed!");
        return NULL;
    }

    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);

    GLFWwindow *window = glfwCreateWindow(state->width, state->height, "minimax", NULL, NULL);

    if (!window)
    {
        DBG("glfwCreateWindow failed!");
        glfwTerminate();
        return NULL;
    }

    return window;
}

void init_bgfx(GLFWwindow *window, State *state)
{

    bgfx::Init init;

    init.type = bgfx::RendererType::Metal;
    init.resolution.width = state->vwidth;
    init.resolution.height = state->vheight;
    init.resolution.reset = BGFX_RESET_VSYNC | BGFX_RESET_HIDPI | BGFX_RESET_MSAA_X4;

#if BX_PLATFORM_LINUX || BX_PLATFORM_BSD
    init.platformData.nwh = (void *)(uintptr_t)glfwGetX11Window(window);
    init.platformData.ndt = glfwGetX11Display();
#elif BX_PLATFORM_OSX
    init.platformData.nwh = glfwGetCocoaWindow(window);
#elif BX_PLATFORM_WINDOWS
    init.platformData.nwh = glfwGetWin32Window(window);
#endif // BX_PLATFORM_
    bgfx::init(init);
}

class Clock
{
private:
    double t;

public:
    double dt;

    Clock()
    {
        t = glfwGetTime();
        dt = 0.016;
    };
    double step()
    {
        double nt = glfwGetTime();
        dt = nt - t;
        t = nt;

        return dt;
    };
};

State *state;
Clock mm_clock;
minimax::ui::Spring sp = minimax::ui::Spring(5, 1, 3, 0, 1);
Scene *scene;

RenderPassGeometry *renderPassGeometry = BX_NEW(&allocator, RenderPassGeometry);
RenderPassUI *renderPassUI = BX_NEW(&allocator, RenderPassUI);
RenderPassCombine *renderPassCombine = BX_NEW(&allocator, RenderPassCombine);

int _width = 800;
int _height = 600;

int frame = 0;

void render()
{
    mm_clock.step();

    if (frame % 30 == 0)
    {
        renderPassGeometry->resize(state->vwidth, state->vheight);
        renderPassUI->resize(state->vwidth, state->vheight);
        renderPassCombine->resize(state->vwidth, state->vheight);
    }

    if (_width != state->width || _height != state->height)
    {
        state->width = _width;
        state->height = _height;
        state->vwidth = _width * state->dpr;
        state->vheight = _height * state->dpr;

        bgfx::reset(state->vwidth, state->vheight, BGFX_RESET_VSYNC | BGFX_RESET_MSAA_X4);

        renderPassGeometry->resize(state->vwidth, state->vheight);
        renderPassUI->resize(state->vwidth, state->vheight);
        renderPassCombine->resize(state->vwidth, state->vheight);
    }

    renderPassGeometry->render(state->vwidth, state->vheight);
    renderPassUI->render(state->vwidth, state->vheight, state->dpr);
    renderPassCombine->render(state->vwidth, state->vheight,
                              renderPassUI->m_texture,
                              renderPassGeometry->m_texColor,
                              renderPassGeometry->m_texPosition,
                              renderPassGeometry->m_texNormal);

    frame = bgfx::frame();
};

void onResize(int width, int height)
{
    _width = width;
    _height = height;

    // renderPassGeometry->resize(state->vwidth, state->vheight);
    // renderPassUI->resize(state->vwidth, state->vheight);
    // renderPassCombine->resize(state->vwidth, state->vheight);

    // render();
};

void renderUI(NVGcontext *vg)
{
    minimax::ui::rect(vg, 16, 16, 100, 100, nvgRGBA(29, 41, 48, 255));
    minimax::ui::rect(vg, 600, 400, 100, 100, nvgRGBA(255, 41, 48, 255));
};

const bgfx::Caps *caps;

void renderScene()
{
    float viewMtx[16];
    float projMtx[16];

    const bx::Vec3 at = {0.0f, 0.0f, 0.0f};
    const bx::Vec3 eye = {0.0f, 1.0f, -2.5f};

    bx::mtxLookAt(viewMtx, eye, at);
    bx::mtxProj(projMtx, 60.0f, state->width / state->height, 0.1f, 100.0f, caps->homogeneousDepth);

    bgfx::setViewClear(0, BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH, state->background_color, 1.0f, 0);

    if (scene)
    {
        for (Mesh *mesh : scene->meshes)
        {
            mesh->submit(renderPassGeometry->m_viewId);
        }
    }
};

int main(int argc, char **argv)
{
    state = create_state();

    // init GLFW window
    GLFWwindow *window = init_glfw(state);

    if (!window)
    {
        return bx::kExitFailure;
    }

    // set framebuffer size and devicePixelRatio
    glfwGetFramebufferSize(window, &state->vwidth, &state->vheight);
    state->dpr = state->vwidth / state->width;

    // setup bgfx
    init_bgfx(window, state);

    // setup ui pass
    renderPassGeometry->init(state->vwidth, state->vheight);
    renderPassUI->init(state->vwidth, state->vheight);
    renderPassCombine->init(state->vwidth, state->vheight);

    if (!renderPassGeometry->m_isValid || !renderPassUI->m_isValid || !renderPassCombine->m_isValid)
    {
        DBG("Initializing render passes failed!");
        delete renderPassGeometry;
        delete renderPassUI;
        delete renderPassCombine;
        delete state;
        bgfx::shutdown();
        glfwDestroyWindow(window);
        glfwTerminate();
        return bx::kExitFailure;
    }

    renderPassGeometry->renderFn = renderScene;
    renderPassUI->renderFn = renderUI;

    caps = bgfx::getCaps();

    // GLFW callbacks
    setup_listeners(window, state, onResize);

    const char *model_file = "/Users/romanliutikov/git/minimax/minimax_c/resources/models/castle.glb";

    scene = load_model(model_file);

    // render loop
    while (!glfwWindowShouldClose(window))
    {
        glfwPollEvents();

        render();
    }

    // shutdown
    delete scene;
    delete state;
    delete renderPassGeometry;
    delete renderPassUI;
    delete renderPassCombine;
    bgfx::shutdown();
    glfwDestroyWindow(window);
    glfwTerminate();

    return bx::kExitSuccess;
}