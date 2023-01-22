(ns minimax.objects.group
  (:require [minimax.object :as obj])
  (:import (org.joml Matrix4f)))

(set! *warn-on-reflection* true)

;; Group node - grouping multiple meshes and/or nodes together
(defrecord Group [name children lmtx mtx]
  obj/IRenderable
  (render [this id]
    (obj/render* id mtx children))
  obj/IObject3D
  (add-child [this child]
    (update this :children conj child))
  (remove-child [this child]
    (update this :children #(filterv (comp not #{child}) %)))
  (children [this]
    children)
  (find-by-name [this name]
    (obj/find-by-name* this name))
  (find-by-type [this obj-type]
    (obj/find-by-type* this obj-type))
  (replace-by-name [this name obj]
    (obj/replace-by-name* this name obj))
  (apply-transform [this inmtx]
    (obj/apply-matrix* inmtx lmtx mtx))
  (rotate-y [this v]
    (obj/rotate-y* lmtx v)))

(defn create [{:keys [lmtx] :as m}]
  (map->Group
   (assoc m
          :mtx (Matrix4f.)
          :lmtx (or lmtx (Matrix4f.))
          :parent (volatile! nil))))
