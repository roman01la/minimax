(ns minimax.renderer.uniform
  (:require [bgfx.core :as bgfx]
            [minimax.mem :as mem])
  (:import (java.nio FloatBuffer)
           (org.lwjgl.bgfx BGFX)))

(set! *warn-on-reflection* true)

(defprotocol IUniform
  (set-value [this value] [this value num-elements])
  (set-texture [this value stage] [this value stage flags]))

(deftype Uniform [name uniform type]
  IUniform
  (set-value [this value]
    (mem/slet [buff [:float (condp = type
                              BGFX/BGFX_UNIFORM_TYPE_VEC4 4
                              BGFX/BGFX_UNIFORM_TYPE_MAT4 16
                              BGFX/BGFX_UNIFORM_TYPE_MAT3 9)]]
      (.get value ^FloatBuffer buff)
      (bgfx/set-uniform uniform buff)))
  (set-value [this value num-elements]
    (bgfx/set-uniform uniform value num-elements))
  (set-texture [this value stage]
    (bgfx/set-texture stage uniform @value))
  (set-texture [this value stage flags]
    (bgfx/set-texture stage uniform @value flags)))

(defn create
  ([name type]
   (create name type 1))
  ([name type num-elements]
   (let [uniform (bgfx/create-uniform name type num-elements)]
     (Uniform. name uniform type))))
