(ns minimax.ui
  (:require
    [bgfx.core :as bgfx]
    [clojure.java.io :as io]
    [fg.state :as state]
    [minimax.lib :as lib]
    [minimax.passes :as passes]
    [minimax.util.fs :as util.fs]
    [minimax.view :as view]
    [minimax.ui.elements :as ui.els])
  (:import (org.lwjgl.bgfx BGFX)
           (org.lwjgl.nanovg NanoVG NanoVGBGFX)
           (org.lwjgl.util.yoga Yoga)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn load-font! [vg font-name file]
  (let [data (util.fs/load-resource file)]
    (when (= -1 (NanoVG/nvgCreateFontMem ^long vg ^CharSequence font-name data 0))
      (throw (RuntimeException. (str "Failed to load " font-name " font"))))))

(defn create-frame-buffer [vg width height]
  (let [fb (NanoVGBGFX/nvgluCreateFramebuffer vg width height 0)]
    (NanoVGBGFX/nvgluSetViewFramebuffer (:id passes/ui) fb)
    fb))

(def frame-buffer
  (lib/with-lifecycle
    create-frame-buffer
    #(NanoVGBGFX/nvgluDeleteFramebuffer %)
    [@ui.els/vg (:vwidth @state/state) (:vheight @state/state)]))

(def texture
  (lib/with-lifecycle
    #(bgfx/get-texture (.handle %) 0)
    bgfx/destroy-texture
    [@frame-buffer]))

(defn init [width height]
  (let [vg (NanoVGBGFX/nvgCreate false (:id passes/ui) 0)]
    (when-not vg
      (throw (RuntimeException. "Failed to init NanoVG")))
    (reset! ui.els/vg vg)

    (load-font! vg "RobotoSlab-Bold" (io/file (io/resource "fonts/Roboto_Slab/RobotoSlab-Bold.ttf")))

    (view/clear passes/ui (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH) 0xff0000ff)))

(defn render* [opts f]
  (let [el (f)]
    (ui.els/layout el)
    (ui.els/store-layout el)
    (ui.els/mouse el opts)
    (ui.els/draw el)
    (Yoga/YGNodeFreeRecursive (:ynode el))))

(defn render [{:keys [width height dpr] :as opts} render-root]
  (view/rect passes/ui 0 0 (* dpr width) (* dpr height))
  (NanoVG/nvgBeginFrame @ui.els/vg width height dpr)
  (render* opts render-root)
  (NanoVG/nvgEndFrame @ui.els/vg))

(defn shutdown []
  (NanoVGBGFX/nvgDelete @ui.els/vg)
  (reset! ui.els/vg nil))
