(ns minimax.renderer.deferred
  (:require [bgfx.core :as bgfx]
            [minimax.mem :as mem]
            [minimax.renderer.vertex-layout :as vertex-layout])
  (:import (org.lwjgl.bgfx BGFX BGFXTransientIndexBuffer BGFXTransientVertexBuffer)))

(defn create-transient-mesh [layout n-elements vertices indices]
  (let [n-verts (/ (count vertices) n-elements)
        n-indices (count indices)]
    (when (and (= n-verts (bgfx/get-transient-vertex-buffer n-verts layout))
               (= n-indices (bgfx/get-transient-index-buffer n-indices)))
      (mem/slet [vb BGFXTransientVertexBuffer
                 ib BGFXTransientIndexBuffer]
        (let [_ (bgfx/alloc-transient-vertex-buffer vb n-verts layout)
              _ (bgfx/alloc-transient-index-buffer ib n-indices)
              vertex (.data vb)
              index (.data ib)]

          (run! #(.putFloat vertex %) vertices)
          (run! #(.putShort index %) indices)

          (.flip vertex)
          (.flip index)

          (bgfx/set-transient-vertex-buffer 0 vb 0 n-verts)
          (bgfx/set-transient-index-buffer ib 0 n-indices))))))

(def layout
  (vertex-layout/create
   [[BGFX/BGFX_ATTRIB_POSITION 3 BGFX/BGFX_ATTRIB_TYPE_FLOAT]
    [BGFX/BGFX_ATTRIB_TEXCOORD0 2 BGFX/BGFX_ATTRIB_TYPE_FLOAT]]))

(def vertices
  [0 0 0 0 0
   1 0 0 1 0
   1 1 0 1 1
   0 1 0 0 1])

(def indices
  [0 1 2 2 3 0])

(defn screen-space-quad []
  (create-transient-mesh @layout 5 vertices indices))
