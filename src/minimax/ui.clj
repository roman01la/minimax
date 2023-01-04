(ns minimax.ui
  (:require
    [bgfx.core :as bgfx]
    [minimax.program :as program]
    [minimax.uniform :as u]
    [minimax.vertex-layout :as vertex-layout]
    [minimax.passes :as passes]
    [minimax.view :as view])
  (:import (java.nio ByteBuffer)
           (org.joml Matrix4f)
           (org.lwjgl.bgfx BGFX BGFXTransientIndexBuffer BGFXTransientVertexBuffer)
           (org.lwjgl.glfw GLFW)
           (org.lwjgl.nuklear NkAllocator NkBuffer NkContext NkConvertConfig NkDrawNullTexture NkDrawVertexLayoutElement NkDrawVertexLayoutElement$Buffer NkPluginAllocI NkPluginCopyI NkPluginFreeI NkPluginPasteI NkRect NkUserFont Nuklear)
           (org.lwjgl.system MemoryStack MemoryUtil)))

(set! *warn-on-reflection* true)

(def z0-1 (.homogeneousDepth (bgfx/get-caps)))

(defn create-allocator []
  (-> (NkAllocator/create)
      (.alloc (reify NkPluginAllocI
                (invoke [this handle old size]
                  (MemoryUtil/nmemAllocChecked size))))
      (.mfree (reify NkPluginFreeI
                (invoke [this handle ptr]
                  (MemoryUtil/nmemFree ptr))))))

(defn- create-nk-vertex-layout ^NkDrawVertexLayoutElement$Buffer []
  (-> (NkDrawVertexLayoutElement/create (int 4))
      (.position 0)
      (.attribute Nuklear/NK_VERTEX_POSITION)
      (.format Nuklear/NK_FORMAT_FLOAT)
      (.offset 0)

      (.position 1)
      (.attribute Nuklear/NK_VERTEX_TEXCOORD)
      (.format Nuklear/NK_FORMAT_FLOAT)
      (.offset 8)

      (.position 2)
      (.attribute Nuklear/NK_VERTEX_COLOR)
      (.format Nuklear/NK_FORMAT_R8G8B8A8)
      (.offset 16)

      (.position 3)
      (.attribute Nuklear/NK_VERTEX_ATTRIBUTE_COUNT)
      (.format Nuklear/NK_FORMAT_COUNT)
      (.offset 0)

      (.flip)))

(defn- init-clipboard [ctx window]
  (-> (.clip ctx)
      (.copy (reify NkPluginCopyI
               (invoke [this handle text len]
                 (when (pos? len)
                   (let [stack (MemoryStack/stackPush)
                         str (.malloc stack (inc len))]
                     (MemoryUtil/memCopy text (MemoryUtil/memAddress str) len)
                     (.put str len (byte 0))
                     (GLFW/glfwSetClipboardString ^long window ^ByteBuffer str))))))
      (.paste (reify NkPluginPasteI
                (invoke [this handle edit]
                  (when-let [text (GLFW/glfwGetClipboardString window)]
                    (Nuklear/nk_textedit_paste edit text)))))))

(def shader
  (delay (program/create "fs_ui" "vs_ui")))

(def s-texture
  (delay (u/create "s_texture" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(def layout
  (vertex-layout/create
    [[BGFX/BGFX_ATTRIB_POSITION 2 BGFX/BGFX_ATTRIB_TYPE_FLOAT]
     [BGFX/BGFX_ATTRIB_TEXCOORD0 2 BGFX/BGFX_ATTRIB_TYPE_FLOAT]
     [BGFX/BGFX_ATTRIB_COLOR0 4 BGFX/BGFX_ATTRIB_TYPE_UINT8 true]]))

(def ctx
  (delay (NkContext/create)))

(def cmds
  (delay (NkBuffer/create)))

(defn init [window]
  (let [allocator (create-allocator)]

    (when-not (Nuklear/nk_init @ctx allocator nil)
      (throw (RuntimeException. "Error initializing nuklear context")))

    (Nuklear/nk_buffer_init @cmds allocator (* 4 1024))
    (init-clipboard @ctx window)))

(defn- create-config []
  (let [tex-null (NkDrawNullTexture/create)
        stack (MemoryStack/stackPush)
        layout (create-nk-vertex-layout)
        ret (-> (NkConvertConfig/calloc stack)
                (.vertex_layout layout)
                (.vertex_size 20)
                (.vertex_alignment 4)
                (.null_texture tex-null)
                (.circle_segment_count 22)
                (.curve_segment_count 22)
                (.arc_segment_count 22)
                (.global_alpha 1)
                (.shape_AA Nuklear/NK_ANTI_ALIASING_ON)
                (.line_AA Nuklear/NK_ANTI_ALIASING_ON))]
    (MemoryStack/stackPop)
    ret))

(defn- set-view-transform [pass width height]
  (let [mtx (Matrix4f.)]
    (.orthoLH mtx 0 width height 0 0 1000 z0-1 mtx)
    (view/transform pass nil mtx)
    (view/rect pass 0 0 width height)))

(def MAX_VERTEX_COUNT 65536)

(defn- setup-buffers [config]
  (let [n-indices (* 2 MAX_VERTEX_COUNT)
        tvb (BGFXTransientVertexBuffer/create)
        tib (BGFXTransientIndexBuffer/create)
        _ (bgfx/alloc-transient-vertex-buffer tvb MAX_VERTEX_COUNT @layout)
        _ (bgfx/alloc-transient-index-buffer tib n-indices)
        vbuf (NkBuffer/create)
        ebuf (NkBuffer/create)]
    (Nuklear/nk_buffer_init_fixed vbuf (.data tvb))
    (Nuklear/nk_buffer_init_fixed ebuf (.data tib))
    (Nuklear/nk_convert @ctx @cmds vbuf ebuf config)
    [tvb tib]))

(def state
  (bit-or
    0
    BGFX/BGFX_STATE_WRITE_RGB
    BGFX/BGFX_STATE_WRITE_A
    BGFX/BGFX_STATE_BLEND_ALPHA
    BGFX/BGFX_STATE_MSAA))

(defn- process-commands [view-id tvb tib]
  (loop [cmd (Nuklear/nk__draw_begin @ctx @cmds)
         offset 0]
    (when cmd
      (when (pos? (.elem_count cmd))
        (let [xx (max (.x (.clip_rect cmd)) 0)
              yy (max (.y (.clip_rect cmd)) 0)
              cw (max (.w (.clip_rect cmd)) 0)
              ch (max (.h (.clip_rect cmd)) 0)]
          (BGFX/bgfx_set_scissor xx yy cw ch)
          (bgfx/set-state state)
          (bgfx/set-transient-vertex-buffer 0 tvb 0 MAX_VERTEX_COUNT)
          (bgfx/set-transient-index-buffer tib offset (.elem_count cmd))
          (bgfx/submit view-id @shader)))
      (recur
        (Nuklear/nk__draw_next cmd @cmds @ctx)
        (+ offset (.elem_count cmd)))))
  (Nuklear/nk_buffer_clear @cmds)
  (Nuklear/nk_clear @ctx))

(def config
  (delay (create-config)))

(defn render [width height]
  (let [view-id (:id passes/ui)
        _ (set-view-transform passes/ui width height)
        [tvb tib] (setup-buffers @config)]
    (process-commands view-id tvb tib)))

;; UI primitives rendering
(defn render-ui []
  (let [stack (MemoryStack/stackPush)
        rect (NkRect/malloc stack)
        ctx ^NkContext (deref ctx)]
    (Nuklear/nk_begin ctx "Demo" (Nuklear/nk_rect 0 0 200 200 rect) (bit-or Nuklear/NK_WINDOW_BACKGROUND
                                                                            Nuklear/NK_WINDOW_BORDER
                                                                            Nuklear/NK_WINDOW_TITLE))
    (Nuklear/nk_end ctx)
    (MemoryStack/stackPop)))
