#define STB_TRUETYPE_IMPLEMENTATION
#include <stb/stb_truetype.h>
#include "nanovg/nanovg.h"

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

void error_callback(int _error, const char *_description)
{
    DBG("GLFW error %d: %s", _error, _description);
}

GLFWwindow *init_glfw(State state)
{
    glfwSetErrorCallback(error_callback);

    if (!glfwInit())
    {
        DBG("glfwInit failed!");
        return NULL;
    }

    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);

    GLFWwindow *window = glfwCreateWindow(state.width, state.height, "minimax", NULL, NULL);

    if (!window)
    {
        DBG("glfwCreateWindow failed!");
        glfwTerminate();
        return NULL;
    }

    return window;
}

void init_bgfx(GLFWwindow *window, State state)
{
    bgfx::renderFrame();

    bgfx::Init init;

    init.type = bgfx::RendererType::Metal;
    init.resolution.width = state.vwidth;
    init.resolution.height = state.vheight;
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

int32_t createFont(NVGcontext *_ctx, const char *_name, const char *_filePath)
{
    uint32_t size;
    void *data = load(_filePath, &size);
    if (NULL == data)
    {
        return -1;
    }

    return nvgCreateFontMem(_ctx, _name, (uint8_t *)data, size, 0);
}

int main(int argc, char **argv)
{
    State state = create_state();

    // init GLFW window
    GLFWwindow *window = init_glfw(state);

    if (!window)
    {
        return bx::kExitFailure;
    }

    // set framebuffer size and devicePixelRatio
    glfwGetFramebufferSize(window, &state.vwidth, &state.vheight);
    state.dpr = state.vwidth / state.width;

    // GLFW callbacks
    setup_listeners(window, state);

    // setup bgfx
    init_bgfx(window, state);

    const char *model_file = "/Users/romanliutikov/git/minimax/minimax_c/resources/models/castle.glb";

    Scene *scene = load_model(model_file);

    float viewMtx[16];
    float projMtx[16];

    const bx::Vec3 at = {0.0f, 0.0f, 0.0f};
    const bx::Vec3 eye = {0.0f, 1.0f, -2.5f};

    bx::mtxLookAt(viewMtx, eye, at);
    bx::mtxProj(projMtx, 60.0f, state.width / state.height, 0.1f, 100.0f, bgfx::getCaps()->homogeneousDepth);

    bgfx::setViewClear(0, BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH, state.background_color, 1.0f, 0);

    // NanoVG
    NVGcontext *nvg = nvgCreate(1, 0);

    bgfx::setViewMode(0, bgfx::ViewMode::Sequential);

    createFont(nvg, "IBMPlexMono Regular", "/Users/romanliutikov/git/minimax/minimax_c/resources/fonts/IBM_Plex_Mono/IBMPlexMono-Regular.ttf");

    // /NanoVG

    // render loop
    while (!glfwWindowShouldClose(window))
    {
        glfwPollEvents();

        bgfx::setViewRect(0, 0, 0, state.vwidth, state.vheight);
        // bgfx::setViewTransform(0, viewMtx, projMtx);
        bgfx::touch(0);

        nvgBeginFrame(nvg, state.width, state.height, state.dpr);

        nvgEndFrame(nvg);

        if (scene)
        {
            for (Mesh *mesh : scene->meshes)
            {
                mesh->submit(0);
            }
        }

        bgfx::frame();
    }

    // shutdown
    delete scene;
    nvgDelete(nvg);
    bgfx::shutdown();
    glfwDestroyWindow(window);
    glfwTerminate();

    return bx::kExitSuccess;
}