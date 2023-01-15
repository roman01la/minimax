#include <GLFW/glfw3.h>
#include "glfw_listeners.h"

static void key_callback(GLFWwindow *_window, int32_t _key, int32_t _scancode, int32_t _action, int32_t _mods)
{
    switch (_key)
    {
    case GLFW_KEY_ESCAPE:
        glfwSetWindowShouldClose(_window, true);
        break;

    case GLFW_KEY_UNKNOWN:
        return;

    default:
        break;
    }
}

void setup_listeners(GLFWwindow *window)
{
    glfwSetKeyCallback(window, key_callback);
}