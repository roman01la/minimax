(ns fg.model
  (:require [clojure.java.io :as io]
            [fg.material :as mat]
            [minimax.assimp.texture :as t]
            [minimax.objects.group :as obj.group]
            [minimax.objects.mesh :as obj.mesh]
            [minimax.util.scene :as util.scene])
  (:import (org.joml Matrix4f)
           (org.lwjgl PointerBuffer)
           (org.lwjgl.assimp AIMatrix4x4 AIMesh AINode AIScene Assimp)
           (java.nio.file Paths)))

(set! *warn-on-reflection* true)

(defn ^Matrix4f create-m4f [^AIMatrix4x4 m]
  (Matrix4f.
   (.a1 m) (.b1 m) (.c1 m) (.d1 m)
   (.a2 m) (.b2 m) (.c2 m) (.d2 m)
   (.a3 m) (.b3 m) (.c3 m) (.d3 m)
   (.a4 m) (.b4 m) (.c4 m) (.d4 m)))

(defn create-scene-graph* [^PointerBuffer meshes materials ^AINode node]
  (let [name (.dataString (.mName node))
        mesh-handles (.mMeshes node)
        mtx (create-m4f (.mTransformation node))
        cmeshes (->> (range (.mNumMeshes node))
                     (mapv #(-> (.get meshes (.get mesh-handles ^int %))
                                AIMesh/create
                                (obj.mesh/create name materials mtx))))
        children (.mChildren node)
        children (->> (range (.mNumChildren node))
                      (mapv #(->> (AINode/create (.get children ^int %))
                                  (create-scene-graph* meshes materials))))]
    (case (count cmeshes)
      ;; single mesh node — mesh
      1 (util.scene/add-parent-link
         (assoc (first cmeshes) :children children))
      ;; zero mesh node — group/empty
      0 (util.scene/add-parent-link
         (obj.group/create
          {:name name
           :children children
           :lmtx mtx}))
      ;; multi mesh node (mesh with multiple materials) — group
      (util.scene/add-parent-link
       (obj.group/create
        {:name name
           ;; reset local transform on child meshes
         :children (-> (mapv #(assoc % :lmtx (Matrix4f.)) cmeshes)
                       (into children))
         :lmtx mtx})))))

(defn create-scene-graph [^AIScene scene materials]
  (create-scene-graph* (.mMeshes scene) materials (.mRootNode scene)))

;; (defn load-model [^String path]
;;   (let [flags (bit-or Assimp/aiProcess_Triangulate
;;                       Assimp/aiProcess_JoinIdenticalVertices
;;                       #_Assimp/aiProcess_FlipWindingOrder
;;                       Assimp/aiProcess_FlipUVs
;;                       Assimp/aiProcess_MakeLeftHanded
;;                       Assimp/aiProcess_GenBoundingBoxes
;;                       #_Assimp/aiProcess_OptimizeGraph
;;                       Assimp/aiProcess_OptimizeMeshes)
;;         scene (-> (.getPath (io/resource path))
;;                   (Assimp/aiImportFile flags))
;;         textures (t/scene->textures scene)
;;         materials (mat/create-materials scene textures)
;;         graph (create-scene-graph scene materials)]
;;     {:path path
;;      :materials materials
;;      :scene graph}))

(defn- normalize-resource-path [path]
  (-> (io/resource path)
      (.toURI)
      (Paths/get)
      (.toAbsolutePath)
      (.toString)))

(defn load-model [^String path]
  (let [flags (bit-or Assimp/aiProcess_Triangulate
                      Assimp/aiProcess_JoinIdenticalVertices
                      Assimp/aiProcess_FlipUVs
                      Assimp/aiProcess_MakeLeftHanded
                      Assimp/aiProcess_GenBoundingBoxes
                      Assimp/aiProcess_OptimizeMeshes)
        normalized-path (normalize-resource-path path)
        scene (Assimp/aiImportFile normalized-path flags)]
    (when (nil? scene)
      (throw (Exception. (str "Failed to load model: " path 
                             ", Error: " (Assimp/aiGetErrorString)))))
    (let [textures (t/scene->textures scene)
          materials (mat/create-materials scene textures)
          graph (create-scene-graph scene materials)]
      {:path path
       :materials materials
       :scene graph})))
