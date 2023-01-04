(ns fg.passes.combine
  (:require
    [bgfx.core :as bgfx]
    [minimax.objects.camera :as camera]
    [fg.deferred :as d]
    [minimax.object :as obj]
    [minimax.passes :as passes]
    [fg.passes.geometry :as pass.geom]
    [fg.shader :as sd]
    [fg.state :as state]
    [minimax.uniform :as u]
    [minimax.texture :as t]
    [minimax.view :as view])
  (:import (org.joml Vector3f)
           (org.lwjgl.bgfx BGFX)
           (org.lwjgl.system MemoryUtil)))

;; SSAO
(defn lerp [a b f]
  (+ a (* f (- b a))))

(def ssao-kernel
  (let [samples 64
        buff (MemoryUtil/memAllocFloat (* samples 4))]
    (doseq [idx (range samples)]
      (let [sample (Vector3f. (- (* 2 (rand)) 1.0)
                              (- (* 2 (rand)) 1.0)
                              (rand))
            scale (/ idx samples)
            scale (lerp 0.1 1.0 (* scale scale))]
        (.normalize sample)
        (.mul sample ^float (rand))
        (.mul sample ^float scale)
        (.put buff (.x sample))
        (.put buff (.y sample))
        (.put buff (.z sample))
        (.put buff (double 0))))
    (.flip buff)
    buff))

(def ssao-noise
  (let [n 16
        mem (MemoryUtil/memAllocFloat (* n 3))]
    (doseq [_ (range n)]
      (.put mem ^double (- (* 2 (rand)) 1.0))
      (.put mem ^double (- (* 2 (rand)) 1.0))
      (.put mem (double 0)))
    (.flip mem)
    mem))

(def ssao-noise-texture
  (delay
    (t/create-2d
      {:width 4
       :height 4
       :format BGFX/BGFX_TEXTURE_FORMAT_RGBA16F
       :mem (bgfx/make-ref ssao-noise)})))

(def u-ssao-noise
  (delay (u/create "s_ssaoNoise" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(def u-ssao-samples
  (delay (u/create "u_ssao_samples" BGFX/BGFX_UNIFORM_TYPE_VEC4 64)))
;; /SSAO

(def u-tex-screen
  (delay (u/create "s_texScreen" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

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

    (u/set-texture @u-tex-screen pass.geom/screen-texture 0)

    (u/set-value @u-ssao-samples ssao-kernel 64)
    (u/set-texture @u-ssao-noise @ssao-noise-texture 1)

    (u/set-texture @pass.geom/u-tex-position pass.geom/position-texture 2)
    (u/set-texture @pass.geom/u-tex-normal pass.geom/normal-texture 3)

    (d/screen-space-quad)
    (bgfx/submit (:id passes/combine) @combine-shader)))
