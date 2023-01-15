#pragma once

struct State
{
    // window
    int width;
    int height;
    int dpr;
    int is_minimized;
    int is_maximized;

    // resolution
    int vwidth;
    int vheight;

    // mouse
    int mx;
    int my;
    int mouse_button;
    int mouse_button_action;

    // scroll
    int sx;
    int sy;

    // scene
    int background_color;
};

State create_state();