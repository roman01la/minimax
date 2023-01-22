(ns minimax.renderer.texture
  (:refer-clojure :exclude [read])
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref)))

(set! *warn-on-reflection* true)

(defprotocol ITexture
  (read [this dest-buff] [this dest-buff mip])
  (destroy [this]))

(deftype Texture2D [handle]
  ITexture
  (read [this dest-buff]
    (bgfx/read-texture handle dest-buff))
  (read [this dest-buff mip]
    (bgfx/read-texture handle dest-buff mip))
  (destroy [this]
    (bgfx/destroy-texture handle))
  IDeref
  (deref [this]
    handle))

(defn create-2d
  [{:keys [width height mips? n-layers format flags mem]
    :or {n-layers 1
         flags bgfx/TEXTURE_DEFAULT_FLAGS
         mem nil
         mips? false}}]
  (let [handle (bgfx/create-texture-2d width height mips? n-layers format flags mem)]
    (Texture2D. handle)))
