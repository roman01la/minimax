cmake_minimum_required(VERSION 3.0.0)
project(minimax VERSION 0.0.1)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

include(CTest)
enable_testing()

add_subdirectory(deps/glfw)
add_subdirectory(deps/bgfx.cmake)
add_subdirectory(deps/assimp)
add_subdirectory(deps/yoga)

file(GLOB SRCFILES src/*.cpp)
add_executable(minimax ${SRCFILES})

set(ASSIMP_NO_EXPORT ON)
set(ASSIMP_BUILD_ALL_EXPORTERS_BY_DEFAULT OFF)

target_link_libraries(minimax glfw ${GLFW_LIBRARIES} bgfx bx bimg assimp yogacore)

set(CPACK_PROJECT_NAME ${PROJECT_NAME})
set(CPACK_PROJECT_VERSION ${PROJECT_VERSION})
include(CPack)
