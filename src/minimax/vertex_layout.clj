(ns minimax.vertex-layout
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref)
           (org.lwjgl.bgfx BGFXVertexLayout)))

(set! *warn-on-reflection* true)

(defprotocol IVertexLayout
  (begin [this])
  (end [this])
  (add [this attribute n-elements element-type]
       [this attribute n-elements element-type normalized?]))

(deftype VertexLayout [layout]
  IVertexLayout
  (begin [this]
    (bgfx/vertex-layout-begin layout (bgfx/get-renderer-type)))
  (end [this]
    (bgfx/vertex-layout-end layout))
  (add [this attribute n-elements element-type]
    (bgfx/vertex-layout-add layout attribute n-elements element-type))
  (add [this attribute n-elements element-type normalized?]
    (bgfx/vertex-layout-add layout attribute n-elements element-type normalized?))
  IDeref
  (deref [this]
    layout))

(defn create
  ([]
   (create []))
  ([attributes]
   (let [layout (BGFXVertexLayout/create)
         vl (VertexLayout. layout)]
     (when (seq attributes)
       (begin vl)
       (doseq [[attribute n-elements element-type normalized?] attributes]
         (add vl attribute n-elements element-type (true? normalized?)))
       (end vl))
     vl)))
