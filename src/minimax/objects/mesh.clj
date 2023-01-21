(ns minimax.objects.mesh
  (:require
    [bgfx.core :as bgfx]
    [fg.passes.geometry :as pass.geom]
    [minimax.lib :as lib]
    [fg.material :as mat]
    [minimax.mem :as mem]
    [minimax.passes :as passes]
    [fg.passes.shadow :as pass.shadow]
    [minimax.assimp.mesh :as assimp.mesh]
    [minimax.index-buffer :as index-buffer]
    [minimax.object :as obj]
    [minimax.uniform :as u]
    [minimax.vertex-buffer :as vertex-buffer])
  (:import (java.nio FloatBuffer)
           (java.util ArrayList)
           (org.joml Matrix4f Vector3f)
           (org.lwjgl.assimp AIMesh)
           (org.lwjgl.bgfx BGFX BGFXCaps)))

(set! *warn-on-reflection* true)

(def discard-state
  (bit-or
    #_BGFX/BGFX_DISCARD_ALL
    0
    BGFX/BGFX_DISCARD_INDEX_BUFFER
    BGFX/BGFX_DISCARD_VERTEX_STREAMS
    BGFX/BGFX_DISCARD_BINDINGS
    BGFX/BGFX_DISCARD_STATE
    BGFX/BGFX_DISCARD_TRANSFORM))

(def mtx-buff (mem/alloc :float 16))

(defn submit-mesh [id vb vc ib ic shader ^Matrix4f mtx state]
  (let [state (or
                state
                (condp contains? id
                  #{(:id passes/shadow)} pass.shadow/render-state
                  #{(:id passes/geometry)} pass.geom/render-state))]
    (assert state "state should be set")
    (bgfx/set-vertex-buffer 0 vb 0 vc)
    (bgfx/set-index-buffer ib 0 ic)
    (bgfx/set-transform (.get mtx ^FloatBuffer mtx-buff))
    (bgfx/set-state state)
    (bgfx/submit id shader 0 discard-state)))

(def ^BGFXCaps caps (bgfx/get-caps))

(let [sy (if (.originBottomLeft caps) 0.5 -0.5)
      sz (if (.homogeneousDepth caps) 0.5 1.0)
      tz (if (.homogeneousDepth caps) 0.5 0.0)]
  (def crop-mtx
    (Matrix4f.
      0.5 0.0 0.0 0.0
      0.0 sy  0.0 0.0
      0.0 0.0 sz  0.0
      0.5 0.5 tz  1.0)))

;; Mesh node - a mesh ¯\_(ツ)_/¯
(defrecord Mesh [name id pid cast-shadow? state
                 vertex-buffer index-buffer
                 ^Matrix4f lmtx ^Matrix4f mtx material children
                 tvec ^Matrix4f light-mtx
                 bounding-box visible?]
  obj/IRenderable
  (render [this id]
    (when @visible?
      (if (and (= id (:id passes/shadow)) (not cast-shadow?))
        nil ;; skip shadow pass when `cast-shadow?` is set to `false` or :pid is not set
        (let [shader (:shader material)
              shadow-shader (:shadow-shader material)
              program (condp = id
                        (:id passes/shadow) shadow-shader
                        (:id passes/geometry) shader)]
          (assert program "shader should be set")
          (.mul pass.shadow/shadow-mtx ^Matrix4f mtx ^Matrix4f light-mtx)
          (.mul ^Matrix4f crop-mtx ^Matrix4f light-mtx ^Matrix4f light-mtx)
          (mat/update-uniforms material light-mtx)
          (submit-mesh id
                       @vertex-buffer (.size ^ArrayList (:vertices vertex-buffer))
                       @index-buffer (.size ^ArrayList (:indices index-buffer))
                       program
                       mtx
                       state)
          (obj/render* id mtx children)))))
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
    (obj/replace-by-name* this name obj))
  (apply-transform [this inmtx]
    (obj/apply-matrix* inmtx lmtx mtx))
  (rotate-y [this v]
    (obj/rotate-y* lmtx v))
  (position [this]
    (.getTranslation lmtx tvec))
  (set-position [this vec3]
    (.setTranslation lmtx ^Vector3f vec3))
  (set-position [this x y z]
    (.setTranslation lmtx (Vector3f. x y z)))
  (set-position-x [this x]
    (let [pos (obj/position this)]
      (.setTranslation lmtx x (.y pos) (.z pos))))
  (set-position-y [this y]
    (let [pos (obj/position this)]
      (.setTranslation lmtx (.x pos) y (.z pos))))
  (set-position-z [this z]
    (let [pos (obj/position this)]
      (.setTranslation lmtx (.x pos) (.y pos) z))))

(defn create [^AIMesh mesh name materials lmtx]
  (let [parsed-mesh (assimp.mesh/process-mesh mesh)

        mat-idx (.mMaterialIndex mesh)
        material (nth materials mat-idx {})

        vbh (vertex-buffer/create parsed-mesh)
        ibh (index-buffer/create (:indices parsed-mesh))]
    (map->Mesh
      {:name name
       :id (.hashCode mesh)
       :pid (lib/int->rgba (.hashCode mesh))
       :vertex-buffer vbh
       :index-buffer ibh
       :lmtx lmtx
       :mtx (Matrix4f.)
       :light-mtx (Matrix4f.)
       :tvec (Vector3f.)
       :material material
       :children []
       :parent (volatile! nil)
       :cast-shadow? true
       :visible? (volatile! true)
       :bounding-box (:bounding-box parsed-mesh)})))
