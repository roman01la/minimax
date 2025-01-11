#!/usr/bin/env nu

# check if shaderc exists
if (which shaderrrc | lines) == 0 {
    print -e -n "[Error] shaderc is not installed."
    print -e -n "        Please install shaderc from bgfx-tools."
    exit 1
}

# bash version:
# METAL_DIR_NAME="metal"
# DX11_DIR_NAME="dx11"
# SPIRV_DIR_NAME="spirv"
# GLSL_DIR_NAME="glsl"

# METAL_FLAGS="--platform osx --profile metal -O 3"
# DX11_FLAGS="--platform windows -p s_5_0 -O 3 --disasm"
# SPIRV_FLAGS="--platform linux -p spirv --disasm"
# GLSL_FLAGS="--platform linux -p 120 --disasm"

# if [ -z "$PLATFORM" ]; then
#     if [[ "$OSTYPE" == "darwin"* ]]; then
#         PLATFORM=osx
#     elif [[ "$OSTYPE" == "linux-gnu" ]]; then
#         PLATFORM=linux
#     elif [[ "$OSTYPE" == "msys" ]]; then
#         PLATFORM=windows
#     else
#         PLATFORM=windows # WORKAROUND
#     fi
#     echo "Platform: $PLATFORM (auto detected)"
# else
#     echo "Platform: $PLATFORM (specified by env)"
# fi

# if [ "$PLATFORM" == "osx" ]; then
#   NEED_METAL=1
#   NEED_DX11=0
#   NEED_SPIRV=0
#   NEED_GLSL=0
# elif [ "$PLATFORM" == "windows" ]; then
#   NEED_METAL=0
#   NEED_DX11=1
#   NEED_SPIRV=0
#   NEED_GLSL=0
# elif [ "$PLATFORM" == "linux" ]; then
#   NEED_METAL=0
#   NEED_DX11=0
#   NEED_SPIRV=1
#   NEED_GLSL=1
# else
#   echo "Unknown platform: $PLATFORM"
#   exit 1
# fi

# function run_cmd {
#   echo "shaderc $@"
#   shaderc $@
# }

# function compile_shader {
#   file=$1
#   dir=$2
#   type=$3
#   run_cmd -f $file \
#           -i resources/shaders \
#           -o "resources/shaders_out/$2$(basename $file .sc).bin" \
#           --type $type $FLAGS 
# }

# function compile_all {
#   if [ $NEED_METAL -eq 1 ]; then
#     echo "## Compiling $1 for Metal"
#     mkdir -p resources/shaders_out/$METAL_DIR_NAME
#     FLAGS=$METAL_FLAGS
#     compile_shader $1 $METAL_DIR_NAME/ $2
#   fi
#   if [ $NEED_DX11 -eq 1 ]; then
#     echo "## Compiling $1 for DX11"
#     mkdir -p resources/shaders_out/$DX11_DIR_NAME
#     FLAGS=$DX11_FLAGS
#     compile_shader $1 $DX11_DIR_NAME/ $2
#   fi
#   if [ $NEED_SPIRV -eq 1 ]; then
#     echo "## Compiling $1 for SPIRV"
#     mkdir -p resources/shaders_out/$SPIRV_DIR_NAME
#     FLAGS=$SPIRV_FLAGS
#     compile_shader $1 $SPIRV_DIR_NAME/ $2
#   fi
#   if [ $NEED_GLSL -eq 1 ]; then
#     echo "## Compiling $1 for GLSL"
#     mkdir -p resources/shaders_out/$GLSL_DIR_NAME
#     FLAGS=$GLSL_FLAGS
#     compile_shader $1 $GLSL_DIR_NAME/ $2
#   fi
# }

# function compile_all_in_dir {
#   for file in $1*
#   do
#     compile_all $file $2
#   done
# }

# compile_all_in_dir resources/shaders/vs_ vertex
# compile_all_in_dir resources/shaders/fs_ fragment
# compile_all_in_dir resources/shaders/check_2d/vs_ vertex
# compile_all_in_dir resources/shaders/check_2d/fs_ fragment

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
    "windows" => "windows"
    _ => "windows"
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

let NEED_GLSL = match ($PLATFORM) {
    "linux" => true
    _ => false
}

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
    }
}

compile_all_in_dir "resources/shaders/vs_*" "vertex"
compile_all_in_dir "resources/shaders/fs_*" "fragment"
compile_all_in_dir "resources/shaders/check_2d/vs_*" "vertex"
compile_all_in_dir "resources/shaders/check_2d/fs_*" "fragment"


mkdir resources/shaders_out
rm -rf resources/shaders_out/*

# ls resources/shaders/vs_* | each { |file|
#     let basename = ($file.name | path basename | str replace '.sc' '')
#     shaderc -f $file.name -o $"resources/shaders_out/($basename).bin" --type vertex --platform osx --profile metal -O 3
# } | ignore

# ls resources/shaders/fs_* | each { |file|
#     let basename = ($file.name | path basename | str replace '.sc' '')
#     shaderc -f $file.name -o $"resources/shaders_out/($basename).bin" --type fragment --platform osx --profile metal -O 3
# } | ignore

# # check_2d
# mkdir resources/shaders_out/check_2d
# ls resources/shaders/check_2d/vs_* | each { |file|
#     let parent = "resources/shaders/"
#     let basename = ($file.name | path basename | str replace '.sc' '')
#     shaderc -f $file.name -i $"($parent)" -o $"resources/shaders_out/check_2d/($basename).bin" --type vertex --platform osx --profile metal -O 3
# } | ignore
# ls resources/shaders/check_2d/fs_* | each { |file|
#     let parent = "resources/shaders/"
#     let basename = ($file.name | path basename | str replace '.sc' '')
#     shaderc -f $file.name -i $"($parent)" -o $"resources/shaders_out/check_2d/($basename).bin" --type fragment --platform osx --profile metal -O 3
# } | ignore

# # check_3d
# mkdir resources/shaders_out/check_3d
# ls resources/shaders/check_3d/vs_* | each { |file|
#     let parent = "resources/shaders/"
#     let basename = ($file.name | path basename | str replace '.sc' '')
#     shaderc -f $file.name -i $"($parent)" -o $"resources/shaders_out/check_3d/($basename).bin" --type vertex --platform osx --profile metal -O 3
# } | ignore
# ls resources/shaders/check_3d/fs_* | each { |file|
#     let parent = "resources/shaders/"
#     let basename = ($file.name | path basename | str replace '.sc' '')
#     shaderc -f $file.name -i $"($parent)" -o $"resources/shaders_out/check_3d/($basename).bin" --type fragment --platform osx --profile metal -O 3
# } | ignore