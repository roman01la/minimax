(ns minimax.lib
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref)
           (java.nio FloatBuffer)
           (org.joml Matrix4f Vector3f Vector4f)
           (org.lwjgl.system MemoryUtil)))

(set! *warn-on-reflection* true)

(def z0-1 (.homogeneousDepth (bgfx/get-caps)))

(def up (Vector3f. 0 1 0))

(defn ^Matrix4f look-at [^Vector3f at ^Vector3f eye ^Matrix4f m]
  (.setLookAtLH m eye at up)
  m)

(defn ^Matrix4f perspective [fov aspect near far ^Matrix4f m]
  (let [fov-rad (Math/toRadians fov)]
    (.setPerspectiveLH m fov-rad aspect near far z0-1)
    m))

(defn ^Matrix4f ortho [area near far ^Matrix4f m]
  (.orthoLH m (- area) area (- area) area near far z0-1 m)
  m)

(defn ^Matrix4f ortho-screen [^Matrix4f m]
  (.orthoLH m 0 1 1 0 -100 100 z0-1 m)
  m)

(defn set-view-transform [view-id view-mtx proj-mtx]
  (let [view-buff (MemoryUtil/memAllocFloat 16)
        proj-buff (MemoryUtil/memAllocFloat 16)]
    (bgfx/set-view-transform view-id
                             (when view-mtx (.get ^Matrix4f view-mtx ^FloatBuffer view-buff))
                             (when proj-mtx (.get ^Matrix4f proj-mtx ^FloatBuffer proj-buff)))))



(defn int->rgba [i]
  (let [r (/ (bit-shift-right (bit-and i 0x000000ff) 0) 255)
        g (/ (bit-shift-right (bit-and i 0x0000ff00) 8) 255)
        b (/ (bit-shift-right (bit-and i 0x00ff0000) 16) 255)]
    (Vector4f. r g b 1.0)))


(defmacro with-lifecycle
  ([setup-fn deps]
   `(with-lifecycle ~setup-fn ~identity ~deps))
  ([setup-fn cleanup-fn deps]
   `(let [v# (atom nil)
          st# (atom nil)]
      (reify IDeref
        (deref [this#]
          (let [nst# ~deps]
            (when (not= @st# nst#)
              (when-let [v# @v#]
                (~cleanup-fn v#))
              (reset! st# nst#)
              (reset! v# (apply ~setup-fn nst#)))
            @v#))))))

