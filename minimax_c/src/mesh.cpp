#include <bgfx/bgfx.h>
#include <bx/math.h>
#include <assimp/mesh.h>

#include "mesh.h"
#include "dbg.h"

void Mesh::parseVertices(aiMesh *mesh)
{
    numVertices = mesh->mNumVertices;

    // TODO: free, smart pointer
    vertices = (float *)malloc(mesh->mNumVertices * 3 * sizeof(float));
    for (size_t i = 0; i < mesh->mNumVertices; i++)
    {
        aiVector3D vec = mesh->mVertices[i];
        vertices[i * 3] = vec.x;
        vertices[i * 3 + 1] = vec.y;
        vertices[i * 3 + 2] = vec.z;
    }
}
void Mesh::parseIndices(aiMesh *mesh)
{
    numIndices = mesh->mNumFaces * 3;
    // TODO: free, smart pointer
    indices = (int *)malloc(numIndices * sizeof(int));
    for (size_t i = 0; i < mesh->mNumFaces; i++)
    {
        aiFace face = mesh->mFaces[i];

        for (size_t j = 0; j < face.mNumIndices; j++)
        {
            indices[i * 3 + j] = face.mIndices[j];
        }
    }
}
void Mesh::parseNormals(aiMesh *mesh)
{
    if (!mesh->mNormals)
    {
        return;
    }

    // TODO: free, smart pointer
    normals = (float *)malloc(mesh->mNumVertices * 3 * sizeof(float));
    for (size_t i = 0; i < mesh->mNumVertices; i++)
    {
        aiVector3D vec = mesh->mNormals[i];
        normals[i * 3] = vec.x;
        normals[i * 3 + 1] = vec.y;
        normals[i * 3 + 2] = vec.z;
    }
}

void Mesh::parseTextureCoords(aiMesh *mesh)
{
    // reads only from the first slot (texture_coords0)

    // TODO: free, smart pointer
    texture_coords = (float *)malloc(mesh->mNumVertices * 3 * sizeof(float));

    aiVector3D *coords = mesh->mTextureCoords[0];

    if (!coords)
    {
        return;
    }

    for (size_t i = 0; i < mesh->mNumVertices; i++)
    {
        aiVector3D vec = coords[i];
        texture_coords[i * 3] = vec.x;
        texture_coords[i * 3 + 1] = vec.y;
        texture_coords[i * 3 + 2] = vec.z;
    }
}

void Mesh::parseBoundingBox(aiMesh *mesh)
{
    aiAABB aabb = mesh->mAABB;
    aiVector3D min = aabb.mMin;
    aiVector3D max = aabb.mMax;

    bounding_box = BoundingBox();

    bounding_box.min[0] = min.x;
    bounding_box.min[1] = min.y;
    bounding_box.min[2] = min.z;

    bounding_box.max[0] = max.x;
    bounding_box.max[1] = max.y;
    bounding_box.max[2] = max.z;
}

void Mesh::createVertexLayout()
{
    layout
        .begin()
        .add(bgfx::Attrib::Position, 3, bgfx::AttribType::Float);

    if (texture_coords)
    {
        layout.add(bgfx::Attrib::TexCoord0, 2, bgfx::AttribType::Float);
    }

    if (normals)
    {
        layout.add(bgfx::Attrib::Normal, 3, bgfx::AttribType::Float);
    }

    layout.end();
}

void Mesh::createVertexBuffer()
{
    int vertexSize = 3;   // vec3
    int normalSize = 3;   // vec3
    int texCoordSize = 2; // vec2

    int count = numVertices * vertexSize;                     // vertices count
    count += normals ? numVertices * normalSize : 0;          // normals count
    count += texture_coords ? numVertices * texCoordSize : 0; // uv count

    float data[count];

    int stride = vertexSize;                     // vertices stride
    stride += normals ? normalSize : 0;          // normals stride
    stride += texture_coords ? texCoordSize : 0; // uv stride

    for (size_t i = 0; i < numVertices; i += stride)
    {
        size_t idx = i;

        // vertices
        data[idx] = vertices[i];
        data[idx + 1] = vertices[i + 1];
        data[idx + 2] = vertices[i + 2];

        idx += 3;

        if (normals)
        {
            // normals
            data[idx] = normals[i];
            data[idx + 1] = normals[i + 1];
            data[idx + 2] = normals[i + 2];

            idx += 2;
        }

        if (texture_coords)
        {
            // uv coords
            data[idx] = texture_coords[i];
            data[idx + 1] = texture_coords[i + 1];
        }
    }

    const bgfx::Memory *mem = bgfx::makeRef(data, sizeof(data));
    vb = bgfx::createVertexBuffer(mem, layout);
}

void Mesh::createIndexBuffer()
{
    int data[numIndices];
    const bgfx::Memory *mem = bgfx::makeRef(data, sizeof(data));
    ib = bgfx::createIndexBuffer(mem);
}

void Mesh::from(aiMesh *mesh)
{
    parseVertices(mesh);
    parseIndices(mesh);
    parseNormals(mesh);
    parseTextureCoords(mesh);
    parseBoundingBox(mesh);

    createVertexLayout();
    createVertexBuffer();
}

void Mesh::applyTransform(float _mtx[16])
{
    bx::mtxMul(mtx, _mtx, lightMtx);
}

void Mesh::submit(bgfx::ViewId id)
{
    bgfx::setVertexBuffer(0, vb, 0, numVertices * 3);
    bgfx::setIndexBuffer(ib, 0, numIndices);
    bgfx::setTransform(mtx);
    bgfx::setState(BGFX_STATE_DEFAULT);
    bgfx::submit(id, program, BGFX_DISCARD_ALL);
}

void Mesh::destroy()
{
    bgfx::destroy(vb);

    // NOTE: Looks like index buffer gets destroyed automatically?
    // bgfx::destroy(ib);
}