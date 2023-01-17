#pragma once

#include <bgfx/bgfx.h>

const bgfx::Memory *loadMem(const char *_filePath);
void *load(const char *_filePath, uint32_t *_size);