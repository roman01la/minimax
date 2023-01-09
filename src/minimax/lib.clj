(ns minimax.lib
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref IRef)
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



(defn int->rgba
  "Encodes integer as RGBA value in Vector4f"
  [i]
  (let [r (/ (bit-shift-right (bit-and i 0x000000ff) 0) 255)
        g (/ (bit-shift-right (bit-and i 0x0000ff00) 8) 255)
        b (/ (bit-shift-right (bit-and i 0x00ff0000) 16) 255)]
    (Vector4f. r g b 1.0)))

(defn with-lifecycle
  "Sets up reactive dependency on `deps` where `derive-deps-fn` is used to calculate
  a value from `deps`, which is then passed into `setup-fn`. The return value of `setup-fn`
  is passed into `cleanup-fn` (when provided) to clean up state before the next update.

  NOTE: `setup-fn` is not called immediately upon creation,
  instead it's called on the first `deref`, which essentially defers initial `setup-fn`
  to 'read' stage."
  ([key setup-fn deps derive-deps-fn]
   (with-lifecycle key setup-fn nil deps derive-deps-fn))
  ([key setup-fn cleanup-fn deps derive-deps-fn]
   (let [get-state #(apply derive-deps-fn (map deref deps))
         state (atom ::nothing)
         value (atom ::nothing)
         first-deref? (atom false)
         on-change (fn [_ _ _ _]
                     (let [next-state (get-state)]
                       (when (not= @state next-state)
                         (reset! state next-state))))]
     (add-watch state key (fn [_ _ _ v]
                            (when (and (not= ::nothing @value) cleanup-fn)
                              (cleanup-fn @value))
                            (reset! value (apply setup-fn v))))
     (reify
       IDeref
       (deref [this]
         (when (= ::nothing @state)
           (reset! first-deref? true)
           (reset! state (get-state))
           (run! #(add-watch % key on-change) deps))
         @value)
       IRef
       (addWatch [this key callback]
         (add-watch value key callback))
       (removeWatch [this key]
         (remove-watch value key))))))
