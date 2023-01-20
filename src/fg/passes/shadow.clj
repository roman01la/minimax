(ns fg.passes.shadow
  (:require
    [bgfx.core :as bgfx]
    [minimax.objects.camera :as camera]
    [minimax.object :as obj]
    [minimax.passes :as passes]
    [fg.state :as state]
    [minimax.frame-buffer :as fb]
    [minimax.view :as view])
  (:import (org.joml Matrix4f Vector3f)
           (org.lwjgl.bgfx BGFX)))

(def render-state
  (bit-or
    0
    BGFX/BGFX_STATE_WRITE_Z
    BGFX/BGFX_STATE_DEPTH_TEST_LESS
    BGFX/BGFX_STATE_CULL_CW))

(defn create-shadow-map-fb [shadow-size]
  (fb/create
    {:width shadow-size
     :height shadow-size
     :format BGFX/BGFX_TEXTURE_FORMAT_D24
     :flags (bit-or BGFX/BGFX_TEXTURE_RT
                    BGFX/BGFX_SAMPLER_COMPARE_LEQUAL)}))

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
