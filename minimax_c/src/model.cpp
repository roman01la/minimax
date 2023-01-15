#include <assimp/Importer.hpp>
#include <assimp/postprocess.h>
#include <assimp/scene.h>
#include <assimp/texture.h>
#include <bgfx/bgfx.h>

#include "model.h"
#include "dbg.h"
#include "mesh.h"

const aiScene *load_model(const char *path)
{
    Assimp::Importer importer;

    const aiScene *scene = importer.ReadFile(path, aiProcessPreset_TargetRealtime_MaxQuality);

    if (scene)
    {
        DBG("Imported meshes count: %d", scene->mNumMeshes);

        for (size_t i = 0; i < scene->mNumMeshes; i++)
        {
            Mesh m = Mesh();
            m.from(scene->mMeshes[i]);
            m.destroy();
        }

        return scene;
    }
    else
    {
        DBG("%s", importer.GetErrorString());
        return NULL;
    }
}