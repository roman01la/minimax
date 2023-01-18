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
#include "fs.h"
#include "state.h"
#include "glfw_listeners.h"
#include "model.h"
#include "scene.h"
#include "ui.h"

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
    bgfx::renderFrame();

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
NVGcontext *vg;
Clock mm_clock;
minimax::ui::Spring sp = minimax::ui::Spring(5, 1, 3, 0, 1);
Scene *scene;

void render()
{
    mm_clock.step();

    float t = sp.step(mm_clock.dt * 4);

    bgfx::setViewRect(0, 0, 0, state->vwidth, state->vheight);
    // bgfx::setViewTransform(0, viewMtx, projMtx);
    bgfx::touch(0);

    nvgBeginFrame(vg, state->width, state->height, state->dpr);

    minimax::ui::rect(vg, 16, 16, 100, 100 * t, nvgRGBA(29, 41, 48, 255));

    nvgEndFrame(vg);

    if (scene)
    {
        for (Mesh *mesh : scene->meshes)
        {
            mesh->submit(0);
        }
    }

    bgfx::frame();
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

    // setup nanovg
    vg = minimax::ui::init();

    if (!vg)
    {
        DBG("Initializing NanoVG failed!");
        bgfx::shutdown();
        glfwDestroyWindow(window);
        glfwTerminate();
        return bx::kExitFailure;
    }

    // GLFW callbacks
    setup_listeners(window, state, render);

    const char *model_file = "/Users/romanliutikov/git/minimax/minimax_c/resources/models/castle.glb";

    scene = load_model(model_file);

    float viewMtx[16];
    float projMtx[16];

    const bx::Vec3 at = {0.0f, 0.0f, 0.0f};
    const bx::Vec3 eye = {0.0f, 1.0f, -2.5f};

    bx::mtxLookAt(viewMtx, eye, at);
    bx::mtxProj(projMtx, 60.0f, state->width / state->height, 0.1f, 100.0f, bgfx::getCaps()->homogeneousDepth);

    bgfx::setViewClear(0, BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH, state->background_color, 1.0f, 0);

    // render loop
    while (!glfwWindowShouldClose(window))
    {
        glfwPollEvents();

        render();
    }

    // shutdown
    delete scene;
    delete state;
    nvgDelete(vg);
    bgfx::shutdown();
    glfwDestroyWindow(window);
    glfwTerminate();

    return bx::kExitSuccess;
}