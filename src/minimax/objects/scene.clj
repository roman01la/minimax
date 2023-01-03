(ns minimax.objects.scene
  (:require [minimax.object :as obj])
  (:import (minimax.objects.light DirectionalLight)
           (org.joml Matrix4f)))

(set! *warn-on-reflection* true)

(defrecord Scene [name children]
  obj/IRenderable
  (render [this id]
    (let [lights (obj/find-by-type this DirectionalLight)]
      ;; lights are rendered first to set up uniforms
      (run! #(obj/render % id) lights)
      (obj/render* id (Matrix4f.) children)))
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
  (map->Scene {:name name
               :children children}))
