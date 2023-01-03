(ns minimax.shader
  (:require
    [bgfx.core :as bgfx]
    [clojure.java.io :as io])
  (:import (clojure.lang IDeref)
           (java.io File)
           (java.nio DirectByteBuffer)
           (org.lwjgl.system MemoryUtil)))

(set! *warn-on-reflection* true)

(defn ^DirectByteBuffer load-resource [^File f]
  (with-open [is (io/input-stream f)]
    (let [bytes (MemoryUtil/memAlloc (.length f))]
      (doseq [b (.readAllBytes is)]
        (.put bytes ^byte b))
      (.flip bytes)
      bytes)))

(deftype Shader [handle]
  IDeref
  (deref [this]
    handle))

(defn create [path]
  (let [f (io/file (io/resource (str "shaders_out/" path ".bin")))
        handle (bgfx/create-shader (bgfx/make-ref (load-resource f)))]
    (bgfx/set-shader-name handle path)
    (Shader. handle)))
