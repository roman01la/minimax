(ns fg.passes.shadow
  (:require
   [bgfx.core :as bgfx]
   [fg.state :as state]
   [minimax.object :as obj]
   [minimax.objects.camera :as camera]
   [minimax.passes :as passes]
   [minimax.renderer.frame-buffer :as fb]
   [minimax.renderer.texture :as t]
   [minimax.renderer.view :as view])
  (:import (org.joml Matrix4f Vector3f)
           (org.lwjgl.bgfx BGFX)))

; cache value if once calculated
(defn- use-shadow-sampler-impl []
  (let [caps (bgfx/get-caps)
        sup (not= (bit-and BGFX/BGFX_CAPS_TEXTURE_COMPARE_LEQUAL
                           (.supported caps))
            0)]
    (when sup
      (println "using shadow sampler"))
    (when (not sup)
      (println "not using shadow sampler, use shadow_pd instead")
      (println "[WARN] shadow_pd is not tested well"))
      sup))

(def -use-shadow-sampler-val (atom nil))
(defn use-shadow-sampler? []
  (or @-use-shadow-sampler-val
  (reset! -use-shadow-sampler-val (use-shadow-sampler-impl))))

(def render-state
  (delay
    (if (use-shadow-sampler?)
      (bit-or
      0
      BGFX/BGFX_STATE_WRITE_Z
      BGFX/BGFX_STATE_DEPTH_TEST_LESS
      BGFX/BGFX_STATE_CULL_CW)
      (bit-or
      0
      BGFX/BGFX_STATE_WRITE_RGB
      BGFX/BGFX_STATE_WRITE_A
      BGFX/BGFX_STATE_WRITE_Z
      BGFX/BGFX_STATE_DEPTH_TEST_LESS
      BGFX/BGFX_STATE_CULL_CW))))

(defn create-shadow-map-fb-for-sampler [shadow-size]
  (fb/create
   {:width shadow-size
    :height shadow-size
    :format BGFX/BGFX_TEXTURE_FORMAT_D24
    :flags (bit-or BGFX/BGFX_TEXTURE_RT
                   BGFX/BGFX_SAMPLER_COMPARE_LEQUAL)}))

(defn create-shadow-map-fb-for-non-sampler [shadow-size]
  (let [width shadow-size
        height shadow-size
        texture-color (t/create-2d
                       {:width width
                        :height height
                        :format BGFX/BGFX_TEXTURE_FORMAT_RGBA8
                        :flags BGFX/BGFX_TEXTURE_RT})
        texture-depth (t/create-2d
                       {:width width
                        :height height
                        :format BGFX/BGFX_TEXTURE_FORMAT_D24
                        :flags BGFX/BGFX_TEXTURE_RT_WRITE_ONLY})]
    (fb/create-from-textures [texture-color texture-depth] true)))

(defn create-shadow-map-fb [shadow-size]
  (if (use-shadow-sampler?)
    (create-shadow-map-fb-for-sampler shadow-size)
    (create-shadow-map-fb-for-non-sampler shadow-size)))

(def shadow-map-fb
  ;; TODO: Maybe make it dependant on `:shadow-map-size` if the value is dynamic
  (delay (create-shadow-map-fb (:shadow-map-size @state/state))))

(def shadow-map-texture
  ;; TODO: Maybe make it dependant on `shadow-map-fb`
  (delay (bgfx/get-texture @@shadow-map-fb 0)))

(def ortho-camera
  (camera/create-orthographic-camera
   {:area 6
    :near -100
    :far 10}))

(defn update-ortho-view-projection [id camera eye-vec3 at-vec3]
  (camera/look-at camera at-vec3 eye-vec3)
  (obj/render camera id))

(def ^Matrix4f shadow-mtx (Matrix4f.))

(defn setup [d-light]
  (let [shadow-map-size (:shadow-map-size @state/state)]
    (view/rect passes/shadow 0 0 shadow-map-size shadow-map-size)
    (view/frame-buffer passes/shadow @@shadow-map-fb)
    (update-ortho-view-projection (:id passes/shadow) ortho-camera
                                  (.negate ^Vector3f (:position d-light) (Vector3f.))
                                  (:at @state/state)))
  (view/clear passes/shadow
              (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH BGFX/BGFX_CLEAR_STENCIL)
              (:background-color @state/state))
  (.mul ^Matrix4f @(:proj-mtx ortho-camera) ^Matrix4f (:view-mtx ortho-camera) ^Matrix4f shadow-mtx))
