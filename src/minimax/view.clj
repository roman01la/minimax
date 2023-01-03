(ns minimax.view
  (:require [bgfx.core :as bgfx]))

(set! *warn-on-reflection* true)

(defprotocol IView
  (rect [this x y width height])
  (clear [this flags rgba] [this flags rgba depth])
  (frame-buffer [this fb-handle])
  (transform [this view-mtx proj-mtx]))

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
    (bgfx/set-view-transform id view-mtx proj-mtx)))

(defn create [id]
  (View. id))
