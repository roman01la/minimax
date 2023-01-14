(ns minimax.resources
  (:require
    [minimax.pool.static :as pool.static]
    [minimax.pool.dynamic :as pool.dynamic]
    [minimax.ui.context :as ui.ctx])
  (:import (org.lwjgl.nanovg NVGColor NanoVG)))

(set! *warn-on-reflection* true)

(def colors
  (pool.static/create 32
    #(NVGColor/calloc)
    #(.free ^NVGColor %)))

(def images
  (pool.dynamic/create 8
    (fn [path]
      (let [vg @ui.ctx/vg]
        (NanoVG/nvgCreateImage ^long vg ^String path 0)))
    (fn [handle]
      (let [vg @ui.ctx/vg]
        (NanoVG/nvgDeleteImage ^long vg handle)))))
