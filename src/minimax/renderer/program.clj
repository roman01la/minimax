(ns minimax.renderer.program
  (:require
   [bgfx.core :as bgfx]
   [minimax.renderer.shader :as shader])
  (:import (clojure.lang IDeref)))

(set! *warn-on-reflection* true)

(deftype Program [handle f-shader v-shader]
  IDeref
  (deref [this]
    handle))

(defn create [fs-path vs-path]
  (let [fsh (shader/create fs-path)
        vsh (shader/create vs-path)
        handle (bgfx/create-program @vsh @fsh true)]
    (Program. handle fsh vsh)))
