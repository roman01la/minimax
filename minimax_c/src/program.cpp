#include <bgfx/bgfx.h>

#include "fs.h"

bgfx::ShaderHandle loadShader(const char *_name)
{
    bgfx::ShaderHandle handle = bgfx::createShader(loadMem(_name));
    bgfx::setName(handle, _name);

    return handle;
}

bgfx::ProgramHandle loadProgram(const char *_vsName, const char *_fsName)
{
    bgfx::ShaderHandle vsh = loadShader(_vsName);
    bgfx::ShaderHandle fsh = loadShader(_fsName);

    return bgfx::createProgram(vsh, fsh, true);
}