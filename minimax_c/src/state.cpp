#include "state.h"

State create_state()
{
    State state;

    state.width = 800;
    state.height = 600;
    state.is_minimized = false;
    state.is_maximized = false;
    state.background_color = 0xe9fcffff;

    return state;
}