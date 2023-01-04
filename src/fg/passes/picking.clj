(ns fg.passes.picking
  (:require
    [bgfx.core :as bgfx]
    [minimax.objects.camera :as camera]
    [minimax.object :as obj]
    [minimax.passes :as passes]
    [fg.shader :as sd]
    [fg.state :as state]
    [minimax.frame-buffer :as fb]
    [minimax.texture :as t]
    [minimax.uniform :as u]
    [minimax.attachment :as attachment]
    [minimax.view :as view])
  (:import (java.nio FloatBuffer)
           (org.joml Matrix4f Vector3f)
           (org.lwjgl.bgfx BGFX)
           (org.lwjgl.system MemoryUtil)))

(def SIZE 8)

(def ^FloatBuffer blit-data (MemoryUtil/memAllocFloat (* SIZE SIZE)))

(def frame-buffer
  (delay
    (let [attachment1 (attachment/create)
          flags (bit-or 0
                  BGFX/BGFX_TEXTURE_RT
                  BGFX/BGFX_SAMPLER_MIN_POINT
                  BGFX/BGFX_SAMPLER_MAG_POINT
                  BGFX/BGFX_SAMPLER_MIP_POINT
                  BGFX/BGFX_SAMPLER_U_CLAMP
                  BGFX/BGFX_SAMPLER_V_CLAMP)
          texture1 (t/create-2d
                     {:width SIZE
                      :height SIZE
                      :format BGFX/BGFX_TEXTURE_FORMAT_RGBA32F
                      :flags flags})]
      (attachment/init attachment1 @texture1)
      (fb/create-from-attachments [@attachment1] true))))

(def picking-texture
  (delay (fb/texture @frame-buffer 0)))

(def blit-texture
  (delay
    (t/create-2d
      {:width SIZE
       :height SIZE
       :format BGFX/BGFX_TEXTURE_FORMAT_RGBA8
       :flags (bit-or 0
                BGFX/BGFX_TEXTURE_BLIT_DST
                BGFX/BGFX_TEXTURE_READ_BACK
                BGFX/BGFX_SAMPLER_MIN_POINT
                BGFX/BGFX_SAMPLER_MAG_POINT
                BGFX/BGFX_SAMPLER_MIP_POINT
                BGFX/BGFX_SAMPLER_U_CLAMP
                BGFX/BGFX_SAMPLER_V_CLAMP)})))

(def u-id
  (delay (u/create "u_id" BGFX/BGFX_UNIFORM_TYPE_VEC4)))

(def shader
  (sd/load-program "fs_picking" "vs_picking"))

(def camera
  (camera/create-perspective-camera
    {:fov 3
     :aspect 1
     :near 0.1
     :far 100}))

(defn setup [scene-camera]
  (let [view-mtx (:view-mtx @scene-camera)
        proj-mtx (:proj-mtx @scene-camera)
        inv-vp-mtx (.invert (.mul view-mtx proj-mtx (Matrix4f.)))
        mx (/ (:mx @state/state) (:width @state/state))
        my (/ (:my @state/state) (:height @state/state))
        eye (.mulProject (Vector3f. mx my 0) ^Matrix4f inv-vp-mtx (Vector3f.))
        at (.mulProject (Vector3f. mx my 1) ^Matrix4f inv-vp-mtx (Vector3f.))]
    (view/clear passes/picking
      (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH)
      0x000000)
    (view/rect passes/picking 0 0 SIZE SIZE)
    (view/frame-buffer passes/picking @@frame-buffer)
    (camera/look-at camera at eye)
    (obj/render camera (:id passes/picking))))

(def reading? (atom 0))

(defn blit []
  (when (zero? @reading?)
    (bgfx/blit (:id passes/blit)
      @@blit-texture 0 0 0 0
      @picking-texture)
    (->> (t/read @blit-texture blit-data)
         (reset! reading?))))

(defn pick [frame]
  (when (= @reading? frame)
    (reset! reading? 0)
    #_(while (.hasRemaining blit-data)
        (let [r (.get blit-data)
              g (.get blit-data)
              b (.get blit-data)
              a (.get blit-data)]
          (prn
            (+
              r
              (* g 256)
              (* b 256 256)))))
    (.flip blit-data)))
