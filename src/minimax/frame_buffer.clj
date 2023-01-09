(ns minimax.frame-buffer
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref)
           (org.lwjgl.bgfx BGFX BGFXAttachment)))

(set! *warn-on-reflection* true)

(defprotocol IFrameBuffer
  (texture [this attachment-idx])
  (destroy [this]))

(deftype FrameBuffer [handle]
  IFrameBuffer
  (texture [this attachment-idx]
    (bgfx/get-texture handle attachment-idx))
  (destroy [this]
    (bgfx/destroy-frame-buffer handle))
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
    (assert (every? #(BGFX/bgfx_is_frame_buffer_valid handle %) attachments)
            "create-from-attachments: the framebuffer is not valid")
    (FrameBuffer. handle)))
