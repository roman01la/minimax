(ns minimax.resources
  (:require
    [minimax.pool :as pool]
    [minimax.ui.context :as ui.ctx])
  (:import (org.lwjgl.nanovg NVGColor NanoVG)))

(def colors
  (pool/create 32
    #(NVGColor/calloc)
    #(.free ^NVGColor %)))

(def images
  (pool/create-dynamic 8
    (fn [path]
      (let [vg @ui.ctx/vg]
        (NanoVG/nvgCreateImage ^long vg ^String path 0)))
    (fn [handle]
      (let [vg @ui.ctx/vg]
        (NanoVG/nvgDeleteImage ^long vg handle)))))
