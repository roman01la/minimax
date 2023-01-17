#include "mesh.h"
#include "scene.h"

Scene::~Scene()
{
    for (Mesh *mesh : meshes)
    {
        delete mesh;
    }
}