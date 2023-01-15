#include <stdio.h>
#include <bx/bx.h>
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
#include "state.h"
#include "glfw_listeners.h"

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

int main(void)
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

    // render loop
    while (!glfwWindowShouldClose(window))
    {
        glfwPollEvents();

        bgfx::setViewClear(0, BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH, state.background_color, 1.0f, 0);
        bgfx::setViewRect(0, 0, 0, state.vwidth, state.vheight);
        bgfx::touch(0);

        bgfx::frame();
    }

    // shutdown
    bgfx::shutdown();
    glfwDestroyWindow(window);
    glfwTerminate();

    return bx::kExitSuccess;
}