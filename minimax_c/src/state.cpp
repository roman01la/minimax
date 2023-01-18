#include "state.h"
#include "allocator.h"

State *create_state()
{
    State *state = BX_NEW(&allocator, State);

    state->width = 800;
    state->height = 600;
    state->is_minimized = false;
    state->is_maximized = false;
    state->background_color = 0xe9fcffff;

    return state;
}