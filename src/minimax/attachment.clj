(ns minimax.attachment
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref)
           (org.lwjgl.bgfx BGFXAttachment)))

(set! *warn-on-reflection* true)

(defprotocol IAttachment
  (init [this texture-handle]))

(deftype Attachment [handle]
  IAttachment
  (init [this texture-handle]
    (bgfx/attachment-init handle texture-handle))
  IDeref
  (deref [this]
    handle))

(defn create []
  (let [attachment (BGFXAttachment/create)]
    (Attachment. attachment)))
