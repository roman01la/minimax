(ns minimax.object
  (:require [minimax.util.scene :as util.scene])
  (:import (org.joml Matrix4f Vector3f Vector4f)))

(set! *warn-on-reflection* true)

(defprotocol IRenderable
  (render [this id]))

(defprotocol IObject3D
  (add-child [this child])
  (remove-child [this child])
  (children [this])
  (find-by-name [this name])
  (find-by-type [this obj-type])
  (replace-by-name [this name obj])

  (position ^Vector3f [this])
  (set-position [this position] [this x y z])
  (set-position-x [this x])
  (set-position-y [this y])
  (set-position-z [this z])

  (rotation ^Vector4f [this])
  (set-rotation [this rotation] [this x y z w])

  (scale ^Vector3f [this])
  (set-scale [this scale] [this x y z])

  (transform ^Matrix4f [this])
  (set-transform [this transform-mtx])

  (apply-transform [this mtx])
  (rotate-y [this v]))

(defn rotate-y* [^Matrix4f mtx v]
  (.rotateY mtx v))

(defn apply-matrix* [^Matrix4f a ^Matrix4f b ^Matrix4f dest]
  (.mul a b dest))

(defn render* [id ^Matrix4f mtx children]
  (run! #(apply-transform % mtx) children)
  (run! #(render % id) children))

(defn add-child* [this child]
  (util.scene/add-parent-link
    (update this :children conj child)))

(defn remove-child* [this child]
  (update this :children #(filterv (comp not #{child}) %)))

(defn scene->seq [scene]
  (tree-seq #(seq (children %)) children scene))

(defn obj->parent-seq [obj ret]
  (let [parent @(:parent obj)]
    (if parent
      (obj->parent-seq parent (conj ret parent))
      ret)))

(defn find-by-name* [this name]
  (->> (scene->seq this)
       (some #(when (= name (:name %)) %))))

(defn replace-by-name* [this name obj]
  (let [found? (some #(when (= name (:name %)) %) (:children this))]
    (if found?
      (->> (:children this)
           (mapv #(if (= name (:name %)) obj %))
           (assoc this :children))
      (->> (:children this)
           (mapv #(replace-by-name* % name obj))
           (assoc this :children)))))

(defn find-by-type* [this obj-type]
  (->> (scene->seq this)
       (filter #(instance? obj-type %))))
