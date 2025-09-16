(ns minimax.physics.core
  (:require [coffi.mem :as mem]
            [coffi.ffi :as ffi :refer [defcfn]])
  (:import (org.joml Vector3f)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def vec3-struct
  [::mem/struct
   [[:x ::mem/float]
    [:y ::mem/float]
    [:z ::mem/float]]])

(defmethod mem/c-layout ::vec3
  [_vec3]
  (mem/c-layout vec3-struct))


(defmethod mem/serialize-into ::vec3
  [obj _vec3 segment arena]
  (mem/serialize-into {:x (obj 0) :y (obj 1) :z (obj 2)} vec3-struct segment arena))


(defmethod mem/deserialize-from ::vec3
  [segment _vec3]
  (let [result (mem/deserialize-from segment vec3-struct)]
    (Vector3f. (:x result) (:y result) (:z result))))

(defcfn add-force
  "Apply a force in the next physics update"
  add_force [::mem/int ::vec3] ::mem/void)