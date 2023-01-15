#pragma once

struct State
{
    // window
    int width;
    int height;
    int dpr;
    bool is_minimized;
    bool is_maximized;

    // resolution
    int vwidth;
    int vheight;

    // mouse
    int mx;
    int my;

    // scroll
    int sx;
    int sy;

    // scene
    int background_color;
};