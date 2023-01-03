(ns minimax.uniform
  (:require [bgfx.core :as bgfx])
  (:import (java.nio FloatBuffer)
           (org.lwjgl.bgfx BGFX)
           (org.lwjgl.system MemoryUtil)))

(set! *warn-on-reflection* true)

(defprotocol IUniform
  (set-value [this value] [this value num-elements])
  (set-texture [this value stage] [this value stage flags]))

(deftype Uniform [name uniform buff]
  IUniform
  (set-value [this value]
    (.get value ^FloatBuffer buff)
    (bgfx/set-uniform uniform buff))
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
   (let [uniform (bgfx/create-uniform name type num-elements)
         buff (condp = type
                BGFX/BGFX_UNIFORM_TYPE_VEC4 (MemoryUtil/memAllocFloat 4)
                BGFX/BGFX_UNIFORM_TYPE_MAT4 (MemoryUtil/memAllocFloat 16)
                BGFX/BGFX_UNIFORM_TYPE_MAT3 (MemoryUtil/memAllocFloat 9)
                nil)]
     (Uniform. name uniform buff))))
