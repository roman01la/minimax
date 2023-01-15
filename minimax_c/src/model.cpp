#include <vector>
#include <assimp/Importer.hpp>
#include <assimp/postprocess.h>
#include <assimp/scene.h>
#include <assimp/texture.h>
#include <bgfx/bgfx.h>

#include "model.h"
#include "dbg.h"
#include "mesh.h"
#include "program.h"
#include "scene.h"
#include "allocator.h"

Scene *load_model(const char *path)
{
    Assimp::Importer importer;

    const aiScene *scene = importer.ReadFile(path, aiProcessPreset_TargetRealtime_MaxQuality);

    if (scene)
    {
        DBG("Imported meshes count: %d", scene->mNumMeshes);

        bgfx::ProgramHandle program = loadProgram(
            "/Users/romanliutikov/git/minimax/minimax_c/resources/shaders_out/vs_shadow.bin",
            "/Users/romanliutikov/git/minimax/minimax_c/resources/shaders_out/fs_shadow.bin");

        Scene *_scene = BX_NEW(&allocator, Scene);

        for (size_t i = 0; i < scene->mNumMeshes; i++)
        {
            Mesh *m = BX_NEW(&allocator, Mesh);
            m->from(scene->mMeshes[i]);
            m->program = program;
            _scene->meshes.push_back(m);
        }

        return _scene;
    }
    else
    {
        DBG("%s", importer.GetErrorString());
        return NULL;
    }
}