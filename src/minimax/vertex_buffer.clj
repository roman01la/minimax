(ns minimax.vertex-buffer
  (:require
    [bgfx.core :as bgfx]
    [minimax.vertex-layout :as vertex-layout])
  (:import (clojure.lang IDeref)
           (java.nio ByteBuffer)
           (java.util ArrayList)
           (org.lwjgl.bgfx BGFX)
           (org.lwjgl.system MemoryUtil)))

(set! *warn-on-reflection* true)

(defn- alloc-memory
  ^ByteBuffer
  [{:keys [^ArrayList vertices ^ArrayList normals ^ArrayList tangents ^ArrayList texture-coords]}]
  (MemoryUtil/memAlloc (* 4 (+ (.size vertices)
                               (.size normals)
                               (.size tangents)
                               (.size texture-coords)))))

(defn- create-vertex-layout [texture-coords? tangents?]
  (vertex-layout/create
    (cond-> [[BGFX/BGFX_ATTRIB_POSITION 3 BGFX/BGFX_ATTRIB_TYPE_FLOAT]
             [BGFX/BGFX_ATTRIB_NORMAL 3 BGFX/BGFX_ATTRIB_TYPE_FLOAT]]
            tangents? (conj [BGFX/BGFX_ATTRIB_TANGENT 3 BGFX/BGFX_ATTRIB_TYPE_FLOAT])
            texture-coords? (conj [BGFX/BGFX_ATTRIB_TEXCOORD0 2 BGFX/BGFX_ATTRIB_TYPE_FLOAT]))))

(defn- create*
  [{:keys [^ArrayList vertices ^ArrayList normals ^ArrayList tangents ^ArrayList texture-coords] :as opts}]
  (let [mem (alloc-memory opts)
        texture-coords? (pos? (.size texture-coords))
        tangents? (pos? (.size tangents))
        layout (create-vertex-layout texture-coords? tangents?)
        cnt (dec (/ (.size vertices) 3))]
    (loop [idx 0]
      (let [idx3 (* 3 idx)
            idx2 (* 2 idx)]
        ;; vertex coords
        (.putFloat mem (.get vertices idx3))
        (.putFloat mem (.get vertices (inc idx3)))
        (.putFloat mem (.get vertices (+ idx3 2)))

        ;; normal coords
        (.putFloat mem (.get normals idx3))
        (.putFloat mem (.get normals (inc idx3)))
        (.putFloat mem (.get normals (+ idx3 2)))

        ;; tangent coords
        (when tangents?
          (.putFloat mem (.get tangents idx3))
          (.putFloat mem (.get tangents (inc idx3)))
          (.putFloat mem (.get tangents (+ idx3 2))))

        ;; uv coords
        (when texture-coords?
          (.putFloat mem (.get texture-coords idx2))
          (.putFloat mem (.get texture-coords (inc idx2))))

        (when (< idx cnt)
          (recur (inc idx)))))
    (assert (zero? (.remaining mem)) "ByteBuffer size and number of arguments do not match")
    (.flip mem)
    (bgfx/create-vertex-buffer (bgfx/make-ref mem) @layout)))

(defrecord VertexBuffer [buffer vertices normals tangents texture-coords]
  IDeref
  (deref [this]
    buffer))

(defn create [{:keys [vertices normals tangents texture-coords] :as opts}]
  (map->VertexBuffer
    (-> (select-keys opts [:vertices :normals :tangents :texture-coords])
        (assoc :buffer (create* opts)))))
