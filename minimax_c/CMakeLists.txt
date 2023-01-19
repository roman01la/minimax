cmake_minimum_required(VERSION 3.0.0)
project(minimax VERSION 0.0.1)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(BGFX_CONFIG_MULTITHREADED OFF)
set(BGFX_BUILD_TOOLS OFF)
set(BGFX_BUILD_EXAMPLES OFF)
set(BGFX_CUSTOM_TARGETS OFF)

set(ASSIMP_NO_EXPORT ON)
set(ASSIMP_ANDROID_JNIIOSYSTEM OFF)
set(ASSIMP_BUILD_ALL_EXPORTERS_BY_DEFAULT OFF)
set(ASSIMP_BUILD_ASSIMP_TOOLS OFF)
set(ASSIMP_BUILD_SAMPLES OFF)
set(ASSIMP_BUILD_TESTS OFF)
set(ASSIMP_COVERALLS OFF)
set(ASSIMP_INSTALL OFF)
set(ASSIMP_BUILD_DOCS OFF)

include(CTest)
enable_testing()

add_subdirectory(deps/glfw)
add_subdirectory(deps/bgfx.cmake)
add_subdirectory(deps/assimp)
add_subdirectory(deps/yoga)

include_directories(
    src
    ${Assimp_SOURCE_DIR}/include
    ${Assimp_SOURCE_DIR}/code
    deps
    deps/bgfx.cmake/bgfx/3rdparty
    deps/bgfx.cmake/bgfx/examples/common)

file(GLOB SRCFILES
    src/*.cpp
    deps/bgfx.cmake/bgfx/examples/common/nanovg/*.cpp)

add_executable(minimax ${SRCFILES})

target_link_libraries(minimax glfw ${GLFW_LIBRARIES} bgfx bx bimg assimp yogacore)

set(CPACK_PROJECT_NAME ${PROJECT_NAME})
set(CPACK_PROJECT_VERSION ${PROJECT_VERSION})
include(CPack)
