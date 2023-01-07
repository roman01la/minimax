(ns minimax.debug
  (:require
    [fg.material :as mat]
    [minimax.index-buffer :as index-buffer]
    [minimax.objects.mesh :as obj.mesh]
    [minimax.vertex-buffer :as vertex-buffer])
  (:import (java.util ArrayList)
           (org.joml Matrix4f Vector3f Vector4f)
           (org.lwjgl.bgfx BGFX)))

(def indices
  (let [arr (ArrayList.)]
    (doseq [x [0 1 2
               1 3 2
               4 6 5
               5 6 7
               0 2 4
               4 2 6
               1 5 3
               5 7 3
               0 4 1
               4 5 1
               2 3 6
               6 3 7]]
      (.add arr x))
    arr))

(def vertices
  (let [arr (ArrayList.)]
    (doseq [x [-1  1  1
                1  1  1
               -1 -1  1
                1 -1  1
               -1  1 -1
                1  1 -1
               -1 -1 -1
                1 -1 -1]]
      (.add arr x))
    arr))

(def material
  (delay (mat/create-line-material {:color (Vector4f. 0 1 0 1)})))

(def lines-state
  (bit-or
    0
    BGFX/BGFX_STATE_PT_LINES
    BGFX/BGFX_STATE_LINEAA
    BGFX/BGFX_STATE_DEFAULT))

(defn create-debug-box []
  (let [vbh (vertex-buffer/create
              {:vertices vertices
               :normals (ArrayList.)
               :tangents (ArrayList.)
               :texture-coords (ArrayList.)})
        ibh (index-buffer/create indices :line)]
    (obj.mesh/map->Mesh
      {:name "debug box"
       :vertex-buffer vbh
       :index-buffer ibh
       :lmtx (Matrix4f.)
       :mtx (Matrix4f.)
       :light-mtx (Matrix4f.)
       :tvec (Vector3f.)
       :material @material
       :children []
       :parent (volatile! nil)
       :cast-shadow? false
       :state lines-state
       :debug-skip? true
       :visible? (volatile! false)})))

(def debug-box
  (delay (create-debug-box)))

(defn set-object-transform [source target ^Matrix4f cmtx]
  (let [{:keys [^Vector3f min ^Vector3f max]} (:bounding-box source)
        a0 (* 0.5 (- (.x max) (.x min)))
        a5 (* 0.5 (- (.y max) (.y min)))
        a10 (* 0.5 (- (.z max) (.z min)))
        a12 (* 0.5 (+ (.x min) (.x max)))
        a13 (* 0.5 (+ (.y min) (.y max)))
        a14 (* 0.5 (+ (.z min) (.z max)))
        mtx (Matrix4f.
              a0  0   0   0
              0   a5  0   0
              0   0   a10 0
              a12 a13 a14 1)
        ^Matrix4f lmtx (:lmtx target)]
    (.set lmtx mtx)
    (.mul cmtx lmtx lmtx)))
