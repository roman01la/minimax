#!/usr/bin/env nu

mkdir resources/shaders_out
rm resources/shaders_out/*

ls resources/shaders/vs_* | each { |file|
    let basename = ($file.name | path basename | str replace '.sc' '')
    shaderc -f $file.name -o $"resources/shaders_out/($basename).bin" --type vertex --platform osx --profile metal -O 3
} | ignore

ls resources/shaders/fs_* | each { |file|
    let basename = ($file.name | path basename | str replace '.sc' '')
    shaderc -f $file.name -o $"resources/shaders_out/($basename).bin" --type fragment --platform osx --profile metal -O 3
} | ignore