(ns minimax.ui
  (:require
    [bgfx.core :as bgfx]
    [clojure.java.io :as io]
    [fg.state :as state]
    [minimax.lib :as lib]
    [minimax.passes :as passes]
    [minimax.ui.context :as ui.ctx]
    [minimax.ui.components :as mui]
    [minimax.ui.elements :as ui]
    [minimax.ui.primitives :as ui.pmt]
    [minimax.util.fs :as util.fs]
    [minimax.view :as view]
    [minimax.logger :as log])
  (:import (org.lwjgl.bgfx BGFX)
           (org.lwjgl.nanovg NanoVG NanoVGBGFX)
           (org.lwjgl.util.yoga Yoga)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn load-font! [vg font-name file]
  (let [data (util.fs/load-resource file)]
    (when (= -1 (NanoVG/nvgCreateFontMem ^long vg ^CharSequence font-name data 1))
      (throw (RuntimeException. (str "Failed to load " font-name " font"))))))

(defn create-frame-buffer [width height]
  (let [textures [(bgfx/create-texture-2d width height false 1
                                          BGFX/BGFX_TEXTURE_FORMAT_RGBA8
                                          BGFX/BGFX_TEXTURE_RT)
                  (bgfx/create-texture-2d width height false 1
                                          BGFX/BGFX_TEXTURE_FORMAT_D24S8
                                          (bit-or BGFX/BGFX_TEXTURE_RT
                                                  BGFX/BGFX_TEXTURE_RT_WRITE_ONLY))]
        fbh (bgfx/create-frame-buffer-from-textures textures true)]
    (bgfx/set-view-frame-buffer (:id passes/ui) fbh)
    (bgfx/set-view-mode (:id passes/ui) BGFX/BGFX_VIEW_MODE_SEQUENTIAL)
    fbh))

(def frame-buffer
  (lib/with-lifecycle
    :frame-buffer
    #(create-frame-buffer %1 %2)
    #(NanoVGBGFX/nvgluDeleteFramebuffer %)
    [state/state]
    (juxt :vwidth :vheight)))

(def texture
  (lib/with-lifecycle
    :texture
    #(bgfx/get-texture % 0)
    bgfx/destroy-texture
    [frame-buffer]
    (fn [fb] [fb])))

(def fonts
  [["Roboto_Slab" "RobotoSlab-Bold"]
   ["IBM_Plex_Mono" "IBMPlexMono-Regular"]
   ["IBM_Plex_Mono" "IBMPlexMono-Bold"]])

(defn load-fonts! [vg]
  (doseq [[dir file] fonts]
    (load-font! vg file (io/file (io/resource (str "fonts/" dir "/" file ".ttf"))))))

(defn init []
  (let [vg (NanoVGBGFX/nvgCreate false (:id passes/ui) 0)]
    (when-not vg
      (throw (RuntimeException. "Failed to init NanoVG")))
    (reset! ui.ctx/vg vg)

    ;; loading UI fonts
    (log/debug "Loading fonts...")
    (time (load-fonts! vg))

    (view/clear passes/ui (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH) 0x00000000)))

(def layout-time (volatile! 0))

(defmacro measure-time [ref & body]
  `(let [start# (System/nanoTime)
         ret# (do ~@body)
         end# (System/nanoTime)
         t# (- end# start#)]
     (vreset! ~ref t#)
     ret#))

(def !root (atom nil))

(defn render* [opts f]
  (reset! mui/!id 0)
  (let [el (f)]
    (reset! !root el)
    (measure-time layout-time
      (ui.pmt/layout (:vnode el)))
    (ui.pmt/store-layout (:vnode el))
    (ui/ui-event el opts)
    (ui.pmt/draw (:vnode el))
    (Yoga/YGNodeFreeRecursive (-> el :vnode :ynode))))

(defn render [{:keys [width height dpr] :as opts} render-root]
  (view/rect passes/ui 0 0 (* dpr width) (* dpr height))
  (NanoVG/nvgBeginFrame @ui.ctx/vg width height dpr)
  (render* opts render-root)
  (NanoVG/nvgEndFrame @ui.ctx/vg))

(defn shutdown []
  (NanoVGBGFX/nvgDelete @ui.ctx/vg)
  (reset! ui.ctx/vg nil))
