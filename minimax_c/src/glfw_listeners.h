#pragma once

#include <GLFW/glfw3.h>
#include "state.h"

static void key_callback(GLFWwindow *_window, int32_t _key, int32_t _scancode, int32_t _action, int32_t _mods);
static void window_resize_callback(GLFWwindow *window, int width, int height);
static void framebuffer_resize_callback(GLFWwindow *window, int width, int height);
static void cursor_position_callback(GLFWwindow *window, double xpos, double ypos);
static void mouse_button_callback(GLFWwindow *window, int button, int action, int mods);
static void scroll_callback(GLFWwindow *window, double xoffset, double yoffset);
static void minimize_callback(GLFWwindow *window, int iconified);
static void maximize_callback(GLFWwindow *window, int maximized);

void setup_listeners(GLFWwindow *window, State *state, void onResize(int, int));