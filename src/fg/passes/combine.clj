(ns fg.passes.combine
  (:require
   [bgfx.core :as bgfx]
   [fg.passes.geometry :as pass.geom]
   [fg.shader :as sd]
   [fg.state :as state]
   [minimax.object :as obj]
   [minimax.objects.camera :as camera]
   [minimax.passes :as passes]
   [minimax.renderer.deferred :as d]
   [minimax.renderer.ui :as ui]
   [minimax.renderer.uniform :as u]
   [minimax.renderer.view :as view])
  (:import (org.lwjgl.bgfx BGFX)))

(def u-tex-screen
  (delay (u/create "s_texScreen" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(def u-tex-position
  (delay (u/create "s_texPosition" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(def u-tex-normal
  (delay (u/create "s_texNormal" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(def u-tex-ui
  (delay (u/create "s_texUI" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(def combine-shader
  (sd/load-program "fs_combine" "vs_combine"))

(def screen-camera
  (camera/create-screen-camera))

(defn render []
  (let [v-width (:vwidth @state/state)
        v-height (:vheight @state/state)]
    (view/rect passes/combine 0 0 v-width v-height)
    (bgfx/set-state
     (bit-or 0
             BGFX/BGFX_STATE_WRITE_RGB
             BGFX/BGFX_STATE_WRITE_A))
    (obj/render screen-camera (:id passes/combine))

    (u/set-texture @u-tex-ui ui/texture 0)
    (u/set-texture @u-tex-screen pass.geom/screen-texture 1)

    (u/set-texture @u-tex-position pass.geom/position-texture 2)
    (u/set-texture @u-tex-normal pass.geom/normal-texture 3)

    (d/screen-space-quad)
    (bgfx/submit (:id passes/combine) @combine-shader)))