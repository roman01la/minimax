#pragma once

#include <GLFW/glfw3.h>

static void key_callback(GLFWwindow *_window, int32_t _key, int32_t _scancode, int32_t _action, int32_t _mods);

void setup_listeners(GLFWwindow *window);