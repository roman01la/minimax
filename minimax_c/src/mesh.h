#pragma once

#include <bgfx/bgfx.h>
#include <assimp/mesh.h>

struct BoundingBox
{
    float min[3];
    float max[3];
};

class Mesh
{
private:
    bgfx::VertexBufferHandle vb;
    bgfx::IndexBufferHandle ib;

    bgfx::VertexLayout layout;

    int numVertices;
    int numIndices;

    float *vertices;
    int *indices;
    float *normals;
    float *tangents;
    float *texture_coords;

    BoundingBox bounding_box;

    float localMtx[16];
    float mtx[16];
    float lightMtx[16];

    bgfx::ProgramHandle program;

public:
    void parseVertices(aiMesh *mesh);
    void parseIndices(aiMesh *mesh);
    void parseNormals(aiMesh *mesh);
    void parseTextureCoords(aiMesh *mesh);

    void parseBoundingBox(aiMesh *mesh);

    void createVertexLayout();
    void createVertexBuffer();
    void createIndexBuffer();

    void from(aiMesh *mesh);

    void applyTransform(float mtx[16]);
    void submit(bgfx::ViewId id);

    void destroy();
};