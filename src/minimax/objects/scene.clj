(ns minimax.objects.scene
  (:require [minimax.object :as obj]
            [minimax.util.scene :as util.scene])
  (:import (minimax.objects.light DirectionalLight)
           (org.joml Matrix4f)))

(set! *warn-on-reflection* true)

(defrecord Scene [name children mtx]
  obj/IRenderable
  (render [this id]
    (let [lights (obj/find-by-type this DirectionalLight)]
      ;; lights are rendered first to set up uniforms
      (run! #(obj/render % id) lights)
      (obj/render* id mtx children)))
  obj/IObject3D
  (add-child [this child]
    (obj/add-child* this child))
  (remove-child [this child]
    (obj/remove-child* this child))
  (children [this]
    children)
  (find-by-name [this name]
    (obj/find-by-name* this name))
  (find-by-type [this obj-type]
    (obj/find-by-type* this obj-type))
  (replace-by-name [this name obj]
    (obj/replace-by-name* this name obj)))

(defn create [{:keys [name children]}]
  (util.scene/add-parent-link
   (map->Scene
    {:name name
     :mtx (Matrix4f.)
     :children children
     :parent (volatile! nil)})))
