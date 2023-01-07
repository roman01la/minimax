(ns fg.passes.geometry
  (:require
    [minimax.objects.camera :as camera]
    [minimax.lib :as lib]
    [minimax.object :as obj]
    [minimax.passes :as passes]
    [fg.state :as state]
    [minimax.attachment :as attachment]
    [minimax.texture :as t]
    [minimax.uniform :as u]
    [minimax.frame-buffer :as fb]
    [bgfx.core :as bgfx]
    [minimax.view :as view])
  (:import (org.lwjgl.bgfx BGFX)))

(def render-state
  (bit-or
    0
    BGFX/BGFX_STATE_WRITE_RGB
    BGFX/BGFX_STATE_WRITE_A
    BGFX/BGFX_STATE_WRITE_Z
    BGFX/BGFX_STATE_DEPTH_TEST_LESS
    BGFX/BGFX_STATE_CULL_CW
    BGFX/BGFX_STATE_MSAA))

(defn create-geometry-fb [v-width v-height]
  (let [attachment1 (attachment/create)
        attachment2 (attachment/create)
        attachment3 (attachment/create)
        flags (bit-or 0
                BGFX/BGFX_TEXTURE_RT_MSAA_X4
                BGFX/BGFX_SAMPLER_MIN_POINT
                BGFX/BGFX_SAMPLER_MAG_POINT
                BGFX/BGFX_SAMPLER_MIP_POINT
                BGFX/BGFX_SAMPLER_U_CLAMP
                BGFX/BGFX_SAMPLER_V_CLAMP)
        texture1 (t/create-2d
                   {:width v-width
                    :height v-height
                    :format BGFX/BGFX_TEXTURE_FORMAT_RGBA16
                    :flags flags})
        texture2 (t/create-2d
                   {:width v-width
                    :height v-height
                    :format BGFX/BGFX_TEXTURE_FORMAT_RGBA16F
                    :flags flags})
        texture3 (t/create-2d
                   {:width v-width
                    :height v-height
                    :format BGFX/BGFX_TEXTURE_FORMAT_RGBA16F
                    :flags flags})]
    (attachment/init attachment1 @texture1)
    (attachment/init attachment2 @texture2)
    (attachment/init attachment3 @texture3)
    (fb/create-from-attachments [@attachment1 @attachment2 @attachment3] true)))

(def geometry-fb
  (lib/with-lifecycle
    create-geometry-fb
    fb/destroy
    [(:vwidth @state/state) (:vheight @state/state)]))

(def screen-texture
  (lib/with-lifecycle
    #(bgfx/get-texture %1 0)
    bgfx/destroy-texture
    [@@geometry-fb]))

(def position-texture
  (lib/with-lifecycle
    #(bgfx/get-texture %1 1)
    bgfx/destroy-texture
    [@@geometry-fb]))

(def u-tex-position
  (delay (u/create "s_texPosition" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(def normal-texture
  (lib/with-lifecycle
    #(bgfx/get-texture %1 2)
    bgfx/destroy-texture
    [@@geometry-fb]))

(def u-tex-normal
  (delay (u/create "s_texNormal" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(defn setup [camera]
  (let [v-width (:vwidth @state/state)
        v-height (:vheight @state/state)]
    (view/clear passes/geometry
      (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH)
      (:background-color @state/state))
    (view/rect passes/geometry 0 0 v-width v-height)
    (view/frame-buffer passes/geometry @@geometry-fb)
    (camera/look-at @camera (:at @state/state) (:eye @state/state))
    (obj/render @camera (:id passes/geometry))))
