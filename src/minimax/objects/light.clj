(ns minimax.objects.light
  (:require [minimax.object :as obj]
            [minimax.uniform :as u])
  (:import (org.joml Matrix4f)
           (org.lwjgl.bgfx BGFX)))

(set! *warn-on-reflection* true)

(defrecord DirectionalLight [name position color uniforms mtx lmtx]
  obj/IRenderable
  (render [this id]
    (u/set-value (:position uniforms) position))
  obj/IObject3D
  (children [this])
  (apply-transform [this inmtx]
    (obj/apply-matrix* inmtx lmtx mtx))
  (rotate-y [this v]
    (obj/rotate-y* lmtx v)))

(defn create-directional-light [{:keys [name position color]}]
  (let [u-position (u/create name BGFX/BGFX_UNIFORM_TYPE_VEC4)]
    (map->DirectionalLight
      {:name name
       :color color
       :position position
       :mtx (Matrix4f.)
       :lmtx (Matrix4f.)
       :uniforms {:position u-position}})))
