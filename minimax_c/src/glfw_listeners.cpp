#include <GLFW/glfw3.h>
#include "state.h"
#include "glfw_listeners.h"
#include "dbg.h"

// TODO: pass explicitly instead
State *_state;
void (*_onResize)(int, int);

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

static void window_resize_callback(GLFWwindow *window, int width, int height)
{
    _onResize(width, height);
}

static void cursor_position_callback(GLFWwindow *window, double xpos, double ypos)
{
    _state->mx = xpos;
    _state->my = ypos;
}

static void mouse_button_callback(GLFWwindow *window, int button, int action, int mods)
{
    _state->mouse_button = button;
    _state->mouse_button_action = action;
}

static void scroll_callback(GLFWwindow *window, double xoffset, double yoffset)
{
    _state->sx = xoffset;
    _state->sy = yoffset;
}

static void minimize_callback(GLFWwindow *window, int iconified)
{
    _state->is_minimized = iconified;
}

static void maximize_callback(GLFWwindow *window, int maximized)
{
    _state->is_maximized = maximized;
}

void setup_listeners(GLFWwindow *window, State *state, void onResize(int, int))
{
    _state = state;
    _onResize = onResize;

    glfwSetKeyCallback(window, key_callback);
    glfwSetWindowSizeCallback(window, window_resize_callback);
    glfwSetCursorPosCallback(window, cursor_position_callback);
    glfwSetMouseButtonCallback(window, mouse_button_callback);
    glfwSetScrollCallback(window, scroll_callback);
    glfwSetWindowIconifyCallback(window, minimize_callback);
    glfwSetWindowMaximizeCallback(window, maximize_callback);
}