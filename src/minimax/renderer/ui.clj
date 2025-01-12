(ns minimax.renderer.ui
  (:require
   [bgfx.core :as bgfx]
   [clojure.java.io :as io]
   [fg.state :as state]
   [minimax.lib :as lib]
   [minimax.logger :as log]
   [minimax.passes :as passes]
   [minimax.pool.core :as pool]
   [minimax.renderer.frame-buffer :as fb]
   [minimax.renderer.texture :as t]
   [minimax.renderer.view :as view]
   [minimax.resources :as res]
   [minimax.ui.components :as mui]
   [minimax.ui.context :as ui.ctx]
   [minimax.ui.diff :as diff]
   [minimax.ui.elements :as ui]
   [minimax.ui.elements2 :as ui2]
   [minimax.ui.primitives :as ui.pmt]
   [minimax.util.fs :as util.fs])
  (:import (org.lwjgl.bgfx BGFX)
           (org.lwjgl.nanovg NanoVG NanoVGBGFX)
           (org.lwjgl.util.yoga Yoga)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn load-font! [vg font-name file]
  (let [data (util.fs/load-resource file)]
    (when (= -1 (NanoVG/nvgCreateFontMem ^long vg ^CharSequence font-name data true))
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
    create-frame-buffer
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
    (log/time (load-fonts! vg))))

(def layout-time (volatile! 0))

(defmacro measure-time [ref & body]
  `(let [start# (System/nanoTime)
         ret# (do ~@body)
         end# (System/nanoTime)
         t# (- end# start#)]
     (vreset! ~ref t#)
     ret#))

(def root (diff/create-root))

(defn render* [opts f]
  (reset! mui/!id 0)
  (let [el (-> (diff/render root (f))
               ui2/get-layout)]
    (measure-time layout-time
                  (ui.pmt/layout (:vnode el)))
    (ui.pmt/store-layout (:vnode el))
    (ui/ui-event el opts)
    (ui.pmt/draw (:vnode el))
    (Yoga/YGNodeFreeRecursive (-> el :vnode :ynode))
    (pool/free res/colors)
    (pool/free res/images)))

(defn render [{:keys [^int width ^int height ^int dpr ^int vwidth ^int vheight] :as opts} render-root]
  (view/clear passes/ui (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH) 0x00000000)
  (view/rect passes/ui 0 0 vwidth vheight)
  (view/frame-buffer passes/ui @@frame-buffer)
  (view/mode passes/ui BGFX/BGFX_VIEW_MODE_SEQUENTIAL)
  (NanoVG/nvgBeginFrame @ui.ctx/vg width height dpr)
  (render* opts render-root)
  (NanoVG/nvgEndFrame @ui.ctx/vg))

(defn shutdown []
  (NanoVGBGFX/nvgDelete @ui.ctx/vg)
  (reset! ui.ctx/vg nil))
