(ns minimax.assimp.mesh
  (:import (java.util ArrayList)
           (org.joml Vector3f)
           (org.lwjgl.assimp AIFace AIMesh AIVector3D)))

(set! *warn-on-reflection* true)

(defn process-vertices [^AIMesh mesh]
  (let [m-vertices (.mVertices mesh)
        vertices (ArrayList.)]
    (while (.hasRemaining m-vertices)
      (let [v ^AIVector3D (.get m-vertices)]
        (.add vertices (.x v))
        (.add vertices (.y v))
        (.add vertices (.z v))))
    vertices))

(defn process-indices [^AIMesh mesh]
  (let [faces (.mFaces mesh)
        indices (ArrayList.)]
    (while (.hasRemaining faces)
      (let [findices (.mIndices ^AIFace (.get faces))]
        (while (.hasRemaining findices)
          (.add indices (.get findices)))))
    indices))

(defn process-normals [^AIMesh mesh]
  (let [m-normals (.mNormals mesh)
        normals (ArrayList.)]
    (while (.hasRemaining m-normals)
      (let [v ^AIVector3D (.get m-normals)]
        (.add normals (.x v))
        (.add normals (.y v))
        (.add normals (.z v))))
    normals))

(defn process-tangents [^AIMesh mesh]
  (let [m-tangents (.mTangents mesh)
        tangents (ArrayList.)]
    (when m-tangents
      (while (.hasRemaining m-tangents)
        (let [v ^AIVector3D (.get m-tangents)]
          (.add tangents (.x v))
          (.add tangents (.y v))
          (.add tangents (.z v)))))
    tangents))

(defn process-texture-coords [^AIMesh mesh]
  ;; taking only from the first slot
  (let [addr (.get (.mTextureCoords mesh))
        texture-coords (ArrayList.)]
    (when-not (zero? addr)
      (let [coords (AIVector3D/create addr (.mNumVertices mesh))]
        (while (.hasRemaining coords)
          (let [v ^AIVector3D (.get coords)]
            (.add texture-coords (.x v))
            (.add texture-coords (.y v))
            ;; only 2d textures
            #_(.add texture-coords (.z v))))))
    texture-coords))

(defn process-bounding-box [^AIMesh mesh]
  (let [aabb (.mAABB mesh)
        min (.mMin aabb)
        max (.mMax aabb)]
    {:min (Vector3f. (.x min) (.y min) (.z min))
     :max (Vector3f. (.x max) (.y max) (.z max))}))

(defn process-mesh [^AIMesh mesh]
  (let [vertices (process-vertices mesh)
        indices (process-indices mesh)
        normals (process-normals mesh)
        tangents (process-tangents mesh)
        texture-coords (process-texture-coords mesh)
        bounding-box (process-bounding-box mesh)]
    {:vertices vertices
     :indices indices
     :normals normals
     :tangents tangents
     :texture-coords texture-coords
     :bounding-box bounding-box}))
