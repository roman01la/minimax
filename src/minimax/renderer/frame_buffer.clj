(ns minimax.renderer.frame-buffer
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref)))

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
  ;; TODO: destroying and creating a new fb doesn't work...
  (let [handle (bgfx/create-frame-buffer-from-attachments (map deref attachments) destroy-textures?)]
    (FrameBuffer. handle)))

(defn create-from-textures [textures destroy-textures?]
  (let [handle (bgfx/create-frame-buffer-from-textures (map deref textures) destroy-textures?)]
    (FrameBuffer. handle)))
