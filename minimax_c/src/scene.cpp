#include "mesh.h"
#include "scene.h"

void Scene::destroy()
{
    for (Mesh *mesh : meshes)
    {
        mesh->destroy();
    }
}