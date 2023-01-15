#pragma once

#include <vector>

#include "mesh.h"

class Scene
{
public:
    std::vector<Mesh *> meshes;

    void destroy();
};