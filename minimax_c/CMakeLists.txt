cmake_minimum_required(VERSION 3.0.0)
project(minimax VERSION 0.1.0)

include(CTest)
enable_testing()

add_subdirectory(deps/glfw)
include_directories(deps/glfw/include)

add_subdirectory(deps/bgfx.cmake)
include_directories(deps/bgfx.cmake/bgfx/include)
include_directories(deps/bgfx.cmake/bx/include)
include_directories(deps/bgfx.cmake/bimg/include)

add_executable(minimax src/core.cpp)

target_link_libraries(minimax glfw bgfx bx bimg ${GLFW_LIBRARIES})

set(CPACK_PROJECT_NAME ${PROJECT_NAME})
set(CPACK_PROJECT_VERSION ${PROJECT_VERSION})
include(CPack)
