(ns minimax.renderer.shader
  (:require
   [bgfx.core :as bgfx]
   [clojure.java.io :as io]
   [minimax.util.fs :as util.fs])
  (:import (clojure.lang IDeref)))

(set! *warn-on-reflection* true)

(deftype Shader [handle]
  IDeref
  (deref [this]
    handle))

(defn create [path]
  (let [f (io/file (io/resource (str "shaders_out/" path ".bin")))
        handle (bgfx/create-shader (bgfx/make-ref-release (util.fs/load-resource f)))]
    (bgfx/set-shader-name handle path)
    (Shader. handle)))
