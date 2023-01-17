#include <bgfx/bgfx.h>
#include <bx/file.h>
#include <bx/readerwriter.h>

#include "fs.h"
#include "dbg.h"
#include "allocator.h"

bx::FileReader *fileReader = BX_NEW(&allocator, bx::FileReader);

const bgfx::Memory *loadMem(const char *_filePath)
{
    if (bx::open(fileReader, _filePath))
    {
        uint32_t size = (uint32_t)bx::getSize(fileReader);
        const bgfx::Memory *mem = bgfx::alloc(size + 1);
        bx::read(fileReader, mem->data, size, bx::ErrorAssert{});
        bx::close(fileReader);
        mem->data[mem->size - 1] = '\0';
        return mem;
    }

    DBG("Failed to load %s.", _filePath);
    return NULL;
}

void *load(const char *_filePath, uint32_t *_size)
{
    if (bx::open(fileReader, _filePath))
    {
        uint32_t size = (uint32_t)bx::getSize(fileReader);
        void *data = BX_ALLOC(&allocator, size);
        bx::read(fileReader, data, size, bx::ErrorAssert{});
        bx::close(fileReader);
        if (NULL != _size)
        {
            *_size = size;
        }
        return data;
    }
    else
    {
        DBG("Failed to open: %s.", _filePath);
    }

    if (NULL != _size)
    {
        *_size = 0;
    }

    return NULL;
}