(ns minimax.ui
  (:require
    [bgfx.core :as bgfx]
    [clojure.java.io :as io]
    [fg.state :as state]
    [minimax.frame-buffer :as fb]
    [minimax.lib :as lib]
    [minimax.passes :as passes]
    [minimax.texture :as t]
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
  (let [textures [(t/create-2d
                    {:width width
                     :height height
                     :format BGFX/BGFX_TEXTURE_FORMAT_RGBA8
                     :flags BGFX/BGFX_TEXTURE_RT})
                  (t/create-2d
                    {:width width
                     :height height
                     :format BGFX/BGFX_TEXTURE_FORMAT_D24S8
                     :flags (bit-or BGFX/BGFX_TEXTURE_RT
                                    BGFX/BGFX_TEXTURE_RT_WRITE_ONLY)})]]

    (fb/create-from-textures textures true)))

(def frame-buffer
  (lib/with-lifecycle
    :frame-buffer
    #(create-frame-buffer %1 %2)
    fb/destroy
    [state/state]
    (juxt :vwidth :vheight)))

(def texture
  (lib/with-lifecycle
    :texture
    #(fb/texture % 0)
    bgfx/destroy-texture
    [frame-buffer]
    vector))

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
    (time (load-fonts! vg))))

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

(defn render [{:keys [^int width ^int height ^int dpr ^int vwidth ^int vheight] :as opts} render-root]
  (view/clear passes/ui (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH) 0x00000000)
  (view/frame-buffer passes/ui @@frame-buffer)
  (view/mode passes/ui BGFX/BGFX_VIEW_MODE_SEQUENTIAL)
  (view/rect passes/ui 0 0 vwidth vheight)
  (NanoVG/nvgBeginFrame @ui.ctx/vg width height dpr)
  (render* opts render-root)
  (NanoVG/nvgEndFrame @ui.ctx/vg))

(defn shutdown []
  (NanoVGBGFX/nvgDelete @ui.ctx/vg)
  (reset! ui.ctx/vg nil))
