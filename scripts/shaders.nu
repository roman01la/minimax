#!/usr/bin/env nu

# check if shaderc exists
if (which shaderrrc | lines) == 0 {
    print -e -n "[Error] shaderc is not installed."
    print -e -n "        Please install shaderc from bgfx-tools."
    exit 1
}

mkdir resources/shaders_out
rm -rf resources/shaders_out/*

ls resources/shaders/vs_* | each { |file|
    let basename = ($file.name | path basename | str replace '.sc' '')
    shaderc -f $file.name -o $"resources/shaders_out/($basename).bin" --type vertex --platform osx --profile metal -O 3
} | ignore

ls resources/shaders/fs_* | each { |file|
    let basename = ($file.name | path basename | str replace '.sc' '')
    shaderc -f $file.name -o $"resources/shaders_out/($basename).bin" --type fragment --platform osx --profile metal -O 3
} | ignore

# check_2d
mkdir resources/shaders_out/check_2d
ls resources/shaders/check_2d/vs_* | each { |file|
    let parent = "resources/shaders/"
    let basename = ($file.name | path basename | str replace '.sc' '')
    shaderc -f $file.name -i $"($parent)" -o $"resources/shaders_out/check_2d/($basename).bin" --type vertex --platform osx --profile metal -O 3
} | ignore
ls resources/shaders/check_2d/fs_* | each { |file|
    let parent = "resources/shaders/"
    let basename = ($file.name | path basename | str replace '.sc' '')
    shaderc -f $file.name -i $"($parent)" -o $"resources/shaders_out/check_2d/($basename).bin" --type fragment --platform osx --profile metal -O 3
} | ignore

# check_3d
mkdir resources/shaders_out/check_3d
ls resources/shaders/check_3d/vs_* | each { |file|
    let parent = "resources/shaders/"
    let basename = ($file.name | path basename | str replace '.sc' '')
    shaderc -f $file.name -i $"($parent)" -o $"resources/shaders_out/check_3d/($basename).bin" --type vertex --platform osx --profile metal -O 3
} | ignore
ls resources/shaders/check_3d/fs_* | each { |file|
    let parent = "resources/shaders/"
    let basename = ($file.name | path basename | str replace '.sc' '')
    shaderc -f $file.name -i $"($parent)" -o $"resources/shaders_out/check_3d/($basename).bin" --type fragment --platform osx --profile metal -O 3
} | ignore