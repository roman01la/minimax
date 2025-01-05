(ns minimax.renderer.view
  (:require [bgfx.core :as bgfx]
            [minimax.lib :as lib]))

(set! *warn-on-reflection* true)

(defprotocol IView
  (rect [this x y width height])
  (clear [this flags rgba] [this flags rgba depth])
  (frame-buffer [this fb-handle])
  (transform [this view-mtx proj-mtx])
  (touch [this])
  (mode [this mode]))

(defrecord View [id]
  IView
  (rect [this x y width height]
    (bgfx/set-view-rect id x y width height))
  (clear [this flags rgba]
    (bgfx/set-view-clear id flags rgba))
  (clear [this flags rgba depth]
    (bgfx/set-view-clear id flags rgba depth))
  (frame-buffer [this fb-handle]
    (bgfx/set-view-frame-buffer id fb-handle))
  (transform [this view-mtx proj-mtx]
    (lib/set-view-transform id view-mtx proj-mtx))
  (touch [this]
    (bgfx/touch id))
  (mode [this mode]
    (bgfx/set-view-mode id mode)))

(defn create [id]
  (View. id))
