#!/usr/bin/env nu

# check if shaderc exists
if (which shaderrrc | lines) == 0 {
    print -e -n "[Error] shaderc is not installed."
    print -e -n "        Please install shaderc from bgfx-tools."
    exit 1
}

let METAL_DIR_NAME = "metal"
let DX11_DIR_NAME = "dx11"
let SPIRV_DIR_NAME = "spirv"
let GLSL_DIR_NAME = "glsl"

# let METAL_FLAGS = "--platform osx --profile metal -O 3"
# let DX11_FLAGS = "--platform windows -p s_5_0 -O 3 --disasm"
# let SPIRV_FLAGS = "--platform linux -p spirv --disasm"
# let GLSL_FLAGS = "--platform linux -p 120 --disasm"

let _host = sys host | get name | str downcase
let PLATFORM = match ($_host) {
    "darwin" => "osx"
    "linux" => "linux"
    "ubuntu" => "linux"
    "windows" => "windows"
    _ => "linux" # WORKAROUND
}

print $"Platform: ($PLATFORM) \(auto detected\)"

let NEED_METAL = match ($PLATFORM) {
    "osx" => true
    _ => false
}

let NEED_DX11 = match ($PLATFORM) {
    "windows" => true
    _ => false
}

let NEED_SPIRV = match ($PLATFORM) {
    "linux" => true
    _ => false
}

# let NEED_GLSL = match ($PLATFORM) {
#     "linux" => true
#     _ => false
# }
let NEED_GLSL = false # WORKAROUND
print $"GLSL is currently disabled for shader compatibility issues"

def print_cmd [...args] {
    let joined = $args | str join " "
    let cmd = $"shaderc ($joined)"
    print $cmd
    # eval $cmd # this is not supported in nushell
}

def compile_shader [file dir type target] {
    match ($target) {
        "metal" => (
            (print_cmd "-f" $file "-i" "resources/shaders" "-o" $"resources/shaders_out/($dir)($file | path parse | get stem).bin" "--type" $type "--platform" "osx" "--profile" "metal" "-O" "3");
            (shaderc -f $file -i "resources/shaders" -o $"resources/shaders_out/($dir)($file | path parse | get stem).bin" --type $type --platform osx --profile metal -O 3)
        )
        "dx11" => (
            (print_cmd "-f" $file "-i" "resources/shaders" "-o" $"resources/shaders_out/($dir)($file | path parse | get stem).bin" "--type" $type "--platform" "windows" "-p" "s_5_0" "-O" "3" "--disasm");
            (shaderc -f $file -i "resources/shaders" -o $"resources/shaders_out/($dir)($file | path parse | get stem).bin" --type $type --platform windows -p s_5_0 -O 3 --disasm)
        )
        "spirv" => (
            (print_cmd "-f" $file "-i" "resources/shaders" "-o" $"resources/shaders_out/($dir)($file | path parse | get stem).bin" "--type" $type "--platform" "linux" "-p" "spirv" "--disasm");
            (shaderc -f $file -i "resources/shaders" -o $"resources/shaders_out/($dir)($file | path parse | get stem).bin" --type $type --platform linux -p spirv --disasm)
        )
        "glsl" => (
            (print_cmd "-f" $file "-i" "resources/shaders" "-o" $"resources/shaders_out/($dir)($file | path parse | get stem).bin" "--type" $type "--platform" "linux" "-p" "120" "--disasm");
            (shaderc -f $file -i "resources/shaders" -o $"resources/shaders_out/($dir)($file | path parse | get stem).bin" --type $type --platform linux -p 120 --disasm)
        )
    } | ignore
}

def compile_all [file type] {
    if ($NEED_METAL) {
        print $"## Compiling ($file) for Metal"
        mkdir $"resources/shaders_out/($METAL_DIR_NAME)"
        compile_shader $file $"($METAL_DIR_NAME)/" $type metal
    }
    if ($NEED_DX11) {
        print $"## Compiling ($file) for DX11"
        mkdir $"resources/shaders_out/($DX11_DIR_NAME)"
        compile_shader $file $"($DX11_DIR_NAME)/" $type dx11
    }
    if ($NEED_SPIRV) {
        print $"## Compiling ($file) for SPIRV"
        mkdir $"resources/shaders_out/($SPIRV_DIR_NAME)"
        compile_shader $file $"($SPIRV_DIR_NAME)/" $type spirv
    }
    if ($NEED_GLSL) {
        print $"## Compiling ($file) for GLSL"
        mkdir $"resources/shaders_out/($GLSL_DIR_NAME)"
        compile_shader $file $"($GLSL_DIR_NAME)/" $type glsl
    }
}

def compile_all_in_dir [pattern type] {
    glob $"($pattern)" | each { |file|
        compile_all $file $type
    } | ignore
}

mkdir resources/shaders_out
# rm -rf resources/shaders_out/*

compile_all_in_dir "resources/shaders/vs_*" "vertex"
compile_all_in_dir "resources/shaders/fs_*" "fragment"
compile_all_in_dir "resources/shaders/check_2d/vs_*" "vertex"
compile_all_in_dir "resources/shaders/check_2d/fs_*" "fragment"
compile_all_in_dir "resources/shaders/shadow_pd/fs_*" fragment
compile_all_in_dir "resources/shaders/shadow_pd/vs_*" vertex