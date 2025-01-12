(ns minimax.renderer.shader
  (:require
   [bgfx.core :as bgfx]
   [clojure.java.io :as io]
   [minimax.util.fs :as util.fs])
  (:import
   (org.lwjgl.bgfx BGFX)
   (clojure.lang IDeref)))

(set! *warn-on-reflection* true)

(deftype Shader [handle]
  IDeref
  (deref [this]
    handle))

(defn- renderer_dir_name []
  (let [r (bgfx/get-renderer-type)]
   (cond
     (= r BGFX/BGFX_RENDERER_TYPE_DIRECT3D11) "dx11/"
     (= r BGFX/BGFX_RENDERER_TYPE_DIRECT3D12) "dx11/"
    ;;  (= r BGFX/BGFX_RENDERER_TYPE_OPENGL) "glsl/" ; WORKAROUND: OpenGL shaders are not working now
     (= r BGFX/BGFX_RENDERER_TYPE_METAL) "metal/"
     (= r BGFX/BGFX_RENDERER_TYPE_VULKAN) "spirv/"
     :else (throw (Exception. (str "No shaders supported for " (bgfx/get-renderer-name r) " renderer"))))))

(defn create [path]
  (let [f (io/file (io/resource (str "shaders_out/" (renderer_dir_name) path ".bin")))
        handle (bgfx/create-shader (bgfx/make-ref-release (util.fs/load-resource f)))]
    (bgfx/set-shader-name handle path)
    (Shader. handle)))
