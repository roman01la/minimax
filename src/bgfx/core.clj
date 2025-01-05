(ns bgfx.core
  (:require [minimax.mem :as mem])
  (:import (java.nio FloatBuffer ShortBuffer)
           (org.lwjgl.bgfx BGFX BGFXAttachment BGFXCaps BGFXInit BGFXReleaseFunctionCallback BGFXReleaseFunctionCallbackI BGFXStats)
           (org.lwjgl.system MemoryUtil)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn create-init []
  (doto (BGFXInit/create)
    (BGFX/bgfx_init_ctor)))

(defn init [_init]
  (BGFX/bgfx_init _init))

(defn shutdown []
  (BGFX/bgfx_shutdown))

(defn reset
  ([width height]
   (reset width height BGFX/BGFX_RESET_NONE))
  ([width height flags]
   (reset width height flags BGFX/BGFX_TEXTURE_FORMAT_COUNT))
  ([width height flags format]
   (BGFX/bgfx_reset width height flags format)))

(defn frame
  ([]
   (frame false))
  ([capture?]
   (BGFX/bgfx_frame capture?)))

(defn get-caps ^BGFXCaps []
  (BGFX/bgfx_get_caps))

(defn get-stats ^BGFXStats []
  (BGFX/bgfx_get_stats))

(defn create-uniform
  ([name type]
   (create-uniform name type 1))
  ([name type num]
   (BGFX/bgfx_create_uniform ^CharSequence name ^int type ^int num)))

(defn create-program
  ([vertex-shader-handle fragment-shader-handle]
   (create-program vertex-shader-handle fragment-shader-handle false))
  ([vertex-shader-handle fragment-shader-handle destroy-shaders?]
   (BGFX/bgfx_create_program vertex-shader-handle fragment-shader-handle destroy-shaders?)))

(defn create-shader [mem]
  (BGFX/bgfx_create_shader mem))

(defn set-shader-name [program-handle ^CharSequence name-str]
  (BGFX/bgfx_set_shader_name ^short program-handle name-str))

(def TEXTURE_DEFAULT_FLAGS (bit-or BGFX/BGFX_TEXTURE_NONE BGFX/BGFX_SAMPLER_NONE))

(defn create-texture-2d
  ([width height mips? n-layers format]
   (create-texture-2d width height mips? n-layers format TEXTURE_DEFAULT_FLAGS))
  ([width height mips? n-layers format flags]
   (create-texture-2d width height mips? n-layers format flags nil))
  ([width height mips? n-layers format flags mem]
   (BGFX/bgfx_create_texture_2d width height mips? n-layers format flags mem)))

(defn destroy-texture [handle]
  (BGFX/bgfx_destroy_texture handle))

(defn read-texture
  ([texture-handle dest-buff]
   (read-texture texture-handle dest-buff 0))
  ([texture-handle dest-buff mip]
   (BGFX/bgfx_read_texture texture-handle dest-buff mip)))

(def FRAME_BUFFER_TEXTURE_DEFAULT_FLAGS
  (bit-or BGFX/BGFX_SAMPLER_U_CLAMP BGFX/BGFX_SAMPLER_V_CLAMP))

(defn create-frame-buffer
  ([width height texture-format]
   (create-frame-buffer width height texture-format FRAME_BUFFER_TEXTURE_DEFAULT_FLAGS))
  ([width height texture-format texture-flags]
   (BGFX/bgfx_create_frame_buffer width height texture-format texture-flags)))

(defn create-frame-buffer-from-attachments
  ([attachments]
   (create-frame-buffer-from-attachments attachments false))
  ([attachments destroy-textures?]
   (let [attachments-buff (BGFXAttachment/create (count attachments)) ;; TODO: alloc on stack instead
         _ (run! #(.put attachments-buff ^BGFXAttachment %) attachments)
         _ (.flip attachments-buff)
         handle (BGFX/bgfx_create_frame_buffer_from_attachment attachments-buff destroy-textures?)]
     (assert (every? #(BGFX/bgfx_is_frame_buffer_valid handle %) attachments)
             "create-from-attachments: the framebuffer is not valid")
     handle)))

(defn create-frame-buffer-from-textures
  ([textures]
   (create-frame-buffer-from-textures textures false))
  ([textures destroy-textures?]
   (mem/slet [^ShortBuffer textures-buff [:short (count textures)]]
     (run! #(.put textures-buff ^short %) textures)
     (.flip textures-buff)
     (let [handle (BGFX/bgfx_create_frame_buffer_from_handles textures-buff ^boolean destroy-textures?)]
       (assert (not= handle 0xffff) "create-frame-buffer-from-textures: the framebuffer is not valid")
       handle))))

(defn get-texture [frame-buffer-handle attachment-idx]
  (BGFX/bgfx_get_texture frame-buffer-handle attachment-idx))

(defn destroy-frame-buffer [handle]
  (BGFX/bgfx_destroy_frame_buffer handle))

(defn attachment-init
  ([attachment-handle texture-handle]
   (attachment-init attachment-handle texture-handle BGFX/BGFX_ACCESS_WRITE))
  ([attachment-handle texture-handle access-flags]
   (attachment-init attachment-handle texture-handle access-flags 0))
  ([attachment-handle texture-handle access-flags layer]
   (attachment-init attachment-handle texture-handle access-flags layer 1))
  ([attachment-handle texture-handle access-flags layer n-layers]
   (attachment-init attachment-handle texture-handle access-flags layer n-layers 0))
  ([attachment-handle texture-handle access-flags layer n-layers mip]
   (attachment-init attachment-handle texture-handle access-flags layer n-layers mip BGFX/BGFX_RESOLVE_AUTO_GEN_MIPS))
  ([attachment-handle texture-handle access-flags layer n-layers mip resolve-flags]
   (BGFX/bgfx_attachment_init attachment-handle texture-handle access-flags layer n-layers mip resolve-flags)))

(defn set-view-rect [view-id x y width height]
  (BGFX/bgfx_set_view_rect view-id x y width height))

(defn set-view-clear
  ([view-id flags]
   (set-view-clear view-id flags 0x000000ff))
  ([view-id flags rgba]
   (set-view-clear view-id flags rgba 1))
  ([view-id flags rgba depth]
   (set-view-clear view-id flags rgba depth 0))
  ([view-id flags rgba depth stencil]
   (BGFX/bgfx_set_view_clear view-id flags rgba depth stencil)))

(defn set-view-mode [view-id mode]
  (BGFX/bgfx_set_view_mode view-id mode))

(defn set-view-frame-buffer [view-id frame-buffer-handle]
  (BGFX/bgfx_set_view_frame_buffer view-id frame-buffer-handle))

(defn set-view-transform [view-id ^FloatBuffer view-mtx ^FloatBuffer proj-mtx]
  (BGFX/bgfx_set_view_transform ^int view-id view-mtx proj-mtx))

(defn set-uniform
  ([handle value]
   (set-uniform handle value 1))
  ([handle value num]
   (BGFX/bgfx_set_uniform ^short handle ^FloatBuffer value ^int num)))

(defn set-texture
  ([stage uniform-handle texture-handle]
   (set-texture stage uniform-handle texture-handle 0xffffffff))
  ([stage uniform-handle texture-handle flags]
   (BGFX/bgfx_set_texture stage uniform-handle texture-handle flags)))

(defn set-state
  ([state]
   (set-state state 0))
  ([state rgba]
   (BGFX/bgfx_set_state state rgba)))

(defn set-transform [mtx]
  (BGFX/bgfx_set_transform ^FloatBuffer mtx))

(defn submit
  ([view-id program-handle]
   (submit view-id program-handle 0))
  ([view-id program-handle depth]
   (submit view-id program-handle depth BGFX/BGFX_DISCARD_ALL))
  ([view-id program-handle depth flags]
   (BGFX/bgfx_submit view-id program-handle depth flags)))

(defn create-release-callback []
  (BGFXReleaseFunctionCallback/create
   (proxy [BGFXReleaseFunctionCallback] []
     (invoke [ptr user-data]
       (MemoryUtil/nmemFree ptr)))))

(def release
  (delay (create-release-callback)))

(defn touch [view-id]
  (BGFX/bgfx_touch view-id))

(defn make-ref [mem]
  (BGFX/bgfx_make_ref mem))

(defn make-ref-release
  ([mem]
   (make-ref-release mem @release))
  ([mem f]
   (BGFX/bgfx_make_ref_release mem ^BGFXReleaseFunctionCallbackI f (long 0))))

(defn blit
  ([view-id
    dst-texture-handle dst-mip dst-x dst-y dst-z
    src-texture-handle]
   (blit view-id
         dst-texture-handle dst-mip dst-x dst-y dst-z
         src-texture-handle 0))
  ([view-id
    dst-texture-handle dst-mip dst-x dst-y dst-z
    src-texture-handle src-mip]
   (blit view-id
         dst-texture-handle dst-mip dst-x dst-y dst-z
         src-texture-handle src-mip 0))
  ([view-id
    dst-texture-handle dst-mip dst-x dst-y dst-z
    src-texture-handle src-mip src-x]
   (blit view-id
         dst-texture-handle dst-mip dst-x dst-y dst-z
         src-texture-handle src-mip src-x 0))
  ([view-id
    dst-texture-handle dst-mip dst-x dst-y dst-z
    src-texture-handle src-mip src-x src-y]
   (blit view-id
         dst-texture-handle dst-mip dst-x dst-y dst-z
         src-texture-handle src-mip src-x src-y 0))
  ([view-id
    dst-texture-handle dst-mip dst-x dst-y dst-z
    src-texture-handle src-mip src-x src-y src-z]
   (blit view-id
         dst-texture-handle dst-mip dst-x dst-y dst-z
         src-texture-handle src-mip src-x src-y src-z
         0xffff))
  ([view-id
    dst-texture-handle dst-mip dst-x dst-y dst-z
    src-texture-handle src-mip src-x src-y src-z
    width]
   (blit view-id
         dst-texture-handle dst-mip dst-x dst-y dst-z
         src-texture-handle src-mip src-x src-y src-z
         width 0xffff))
  ([view-id
    dst-texture-handle dst-mip dst-x dst-y dst-z
    src-texture-handle src-mip src-x src-y src-z
    width height]
   (blit view-id
         dst-texture-handle dst-mip dst-x dst-y dst-z
         src-texture-handle src-mip src-x src-y src-z
         width height 0xffff))
  ([view-id
    dst-texture-handle dst-mip dst-x dst-y dst-z
    src-texture-handle src-mip src-x src-y src-z
    width height depth]
   (BGFX/bgfx_blit
    view-id
    dst-texture-handle dst-mip dst-x dst-y dst-z
    src-texture-handle src-mip src-x src-y src-z
    width height depth)))

(defn vertex-layout-begin
  ([layout]
   (vertex-layout-begin layout BGFX/BGFX_RENDERER_TYPE_NOOP))
  ([layout renderer-type]
   (BGFX/bgfx_vertex_layout_begin layout renderer-type)))

(defn vertex-layout-add
  ([layout attribute n-elements element-type]
   (vertex-layout-add layout attribute n-elements element-type false))
  ([layout attribute n-elements element-type normalized?]
   (vertex-layout-add layout attribute n-elements element-type normalized? false))
  ([layout attribute n-elements element-type normalized? as-int?]
   (BGFX/bgfx_vertex_layout_add layout attribute n-elements element-type normalized? as-int?)))

(defn vertex-layout-end [layout]
  (BGFX/bgfx_vertex_layout_end layout))

(defn create-vertex-buffer
  ([mem layout]
   (create-vertex-buffer mem layout BGFX/BGFX_BUFFER_NONE))
  ([mem layout flags]
   (BGFX/bgfx_create_vertex_buffer mem layout flags)))

(defn set-vertex-buffer [stream vb-handle start-vertex n-vertices]
  (BGFX/bgfx_set_vertex_buffer stream vb-handle start-vertex n-vertices))

(defn get-transient-vertex-buffer [n-vertices layout]
  (BGFX/bgfx_get_avail_transient_vertex_buffer n-vertices layout))

(defn alloc-transient-vertex-buffer [tvb n-vertices layout]
  (BGFX/bgfx_alloc_transient_vertex_buffer tvb n-vertices layout))

(defn set-transient-vertex-buffer [stream handle start-vertex n-vertices]
  (BGFX/bgfx_set_transient_vertex_buffer stream handle start-vertex n-vertices))

(defn create-index-buffer
  ([mem]
   (create-index-buffer mem BGFX/BGFX_BUFFER_NONE))
  ([mem flags]
   (BGFX/bgfx_create_index_buffer mem flags)))

(defn set-index-buffer [ib-handle start-index n-indices]
  (BGFX/bgfx_set_index_buffer ib-handle start-index n-indices))

(defn get-transient-index-buffer
  ([n-indices]
   (get-transient-index-buffer n-indices false))
  ([n-indices index32bit?]
   (BGFX/bgfx_get_avail_transient_index_buffer n-indices index32bit?)))

(defn alloc-transient-index-buffer
  ([ib n-indices]
   (alloc-transient-index-buffer ib n-indices false))
  ([ib n-indices index32bit?]
   (BGFX/bgfx_alloc_transient_index_buffer ib n-indices index32bit?)))

(defn set-transient-index-buffer [handle start-index n-indices]
  (BGFX/bgfx_set_transient_index_buffer handle start-index n-indices))

(defn get-renderer-type []
  (BGFX/bgfx_get_renderer_type))

(defn get-renderer-name [renderer-type]
  (BGFX/bgfx_get_renderer_name renderer-type))
