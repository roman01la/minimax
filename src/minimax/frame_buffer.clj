(ns minimax.frame-buffer
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref)
           (org.lwjgl.bgfx BGFXAttachment)))

(set! *warn-on-reflection* true)

(defprotocol IFrameBuffer
  (texture [this attachment-idx]))

(deftype FrameBuffer [handle]
  IFrameBuffer
  (texture [this attachment-idx]
    (bgfx/get-texture handle attachment-idx))
  IDeref
  (deref [this]
    handle))

(defn create [{:keys [width height format flags]}]
  (let [handle (bgfx/create-frame-buffer width height format flags)]
    (FrameBuffer. handle)))

(defn create-from-attachments [attachments destroy-textures?]
  (let [attachments-buff (BGFXAttachment/create (count attachments))
        _ (doseq [x attachments]
            (.put attachments-buff ^BGFXAttachment x))
        _ (.flip attachments-buff)
        handle (bgfx/create-frame-buffer-from-attachment attachments-buff destroy-textures?)]
    (FrameBuffer. handle)))
