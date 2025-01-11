(ns check.check-util
  (:require
   [bgfx.core :as bgfx])
  (:import
   [java.nio ByteBuffer]
   (org.lwjgl.bgfx BGFX BGFXVertexLayout)
   [org.lwjgl.system MemoryUtil]))

(defn byte-size-of
  ([type #_int count]
    (byte-size-of type count 0))
  ([type #_int count #_int num-uvs]
    (case type
        :xyc (+ (* (+ (* 2 4) 4) (int count)) (* (* 2 4 4) (int num-uvs)))
        :xyzc (+ (* (+ (* 3 4) 4) (int count)) (* (* 2 4 4) (int num-uvs)))
        (throw (RuntimeException. "Invalid vertex layout type")))))

(defn create-vertex-layout-2d [with-normals with-color num-uvs]
  (let [layout (BGFXVertexLayout/calloc)]
    (BGFX/bgfx_vertex_layout_begin layout BGFX/BGFX_RENDERER_TYPE_COUNT)
    (BGFX/bgfx_vertex_layout_add layout BGFX/BGFX_ATTRIB_POSITION 2 BGFX/BGFX_ATTRIB_TYPE_FLOAT false false)
    (when with-normals
      (BGFX/bgfx_vertex_layout_add layout BGFX/BGFX_ATTRIB_NORMAL 3 BGFX/BGFX_ATTRIB_TYPE_FLOAT false false))
    (when with-color
      (BGFX/bgfx_vertex_layout_add layout BGFX/BGFX_ATTRIB_COLOR0 4 BGFX/BGFX_ATTRIB_TYPE_UINT8 true false))
    (when (> num-uvs 0)
      (BGFX/bgfx_vertex_layout_add layout BGFX/BGFX_ATTRIB_TEXCOORD0 2 BGFX/BGFX_ATTRIB_TYPE_FLOAT false false))
    (BGFX/bgfx_vertex_layout_end layout)
    layout))

(defn create-vertex-layout-3d [with-normals with-color num-uvs]
  (let [layout (BGFXVertexLayout/calloc)]
    (BGFX/bgfx_vertex_layout_begin layout BGFX/BGFX_RENDERER_TYPE_COUNT)
    (BGFX/bgfx_vertex_layout_add layout BGFX/BGFX_ATTRIB_POSITION 3 BGFX/BGFX_ATTRIB_TYPE_FLOAT false false)
    (when with-normals
      (BGFX/bgfx_vertex_layout_add layout BGFX/BGFX_ATTRIB_NORMAL 3 BGFX/BGFX_ATTRIB_TYPE_FLOAT false false))
    (when with-color
      (BGFX/bgfx_vertex_layout_add layout BGFX/BGFX_ATTRIB_COLOR0 4 BGFX/BGFX_ATTRIB_TYPE_UINT8 true false))
    (when (> num-uvs 0)
      (BGFX/bgfx_vertex_layout_add layout BGFX/BGFX_ATTRIB_TEXCOORD0 2 BGFX/BGFX_ATTRIB_TYPE_FLOAT false false))
    (BGFX/bgfx_vertex_layout_end layout)
    layout))

;; (defn ubyte-from-signed-byte [i]
;;   (if (> i 127)
;;     (byte (- i 256))
;;     (byte i)))

(defn uint32-from-int [i]
  (if (> i 2147483647)
    (int (- i 4294967296))
    (int i)))

(defn uint32 [i]
  (uint32-from-int i))

;; (defn write-uint32-to-buffer [^ByteBuffer buffer ^long i]
;;   ; note: uint32 range is bigger than int
;;   ;       so (int) cannot be used
;;   ; first, separate the 32 bits into 4 x 8 bits using and
;;   ;; then write each 8 bits to the buffer
;;   (let [i32 (long i)
;;     r (bit-shift-right (bit-and i32 0xFF000000) 24)
;;     g (bit-shift-right (bit-and i32 0x00FF0000) 16) 
;;     b (bit-shift-right (bit-and i32 0x0000FF00) 8)
;;     a (bit-and i32 0x000000FF)]
;;     (doto buffer
;;       (.put (byte (ubyte-from-signed-byte r)))
;;       (.put (byte (ubyte-from-signed-byte g)))
;;       (.put (byte (ubyte-from-signed-byte b)))
;;       (.put (byte (ubyte-from-signed-byte a))))))

(defn- create-vertex-buffer-impl
  ([^ByteBuffer buffer ^BGFXVertexLayout layout]
    (let [vbhMem (BGFX/bgfx_make_ref buffer)]
      (BGFX/bgfx_create_vertex_buffer vbhMem layout BGFX/BGFX_BUFFER_NONE))))

(defn create-vertex-buffer
  ([^ByteBuffer buffer ^BGFXVertexLayout layout vertices]
   (doseq [vtx vertices]
     (doseq [attr vtx]
        (cond
            (instance? Float attr) (.putFloat buffer (float attr))
            (instance? Double attr) (.putFloat buffer (float attr)) ; only for clojure support
            (instance? Integer attr) (.putInt buffer attr)
            ;;  (instance? Long attr) (write-uint32-to-buffer buffer attr) ; only for clojure support
            (instance? Long attr) (.putInt buffer (uint32-from-int attr))
            :else (throw (RuntimeException. (str "Invalid parameter type: " (type attr)))))
        ))
   (when (not= (.remaining buffer) 0)
      (throw (RuntimeException. "ByteBuffer size and number of arguments do not match")))
   (.flip buffer)
   (create-vertex-buffer-impl buffer layout)))

(defn create-vertex-buffer-with-size
 ([#_int byte-size ^BGFXVertexLayout layout vertices]
    (let [buffer (MemoryUtil/memAlloc (int byte-size))]
      (create-vertex-buffer buffer layout vertices))))

(defn create-index-buffer
  ([indices]
    (let [buffer (MemoryUtil/memAlloc (int (* (count indices) 2)))]
      (create-index-buffer buffer indices)))
  ([^ByteBuffer buffer indices]
   (let [buffer-size (int (* (count indices) 2))]
     (doseq [idx indices]
       (.putShort buffer (short idx))) 
     (when (not= (.remaining buffer) 0)
       (throw (RuntimeException. "ByteBuffer size and number of arguments do not match")))
     (.flip buffer))
   (let [ibhMem (BGFX/bgfx_make_ref buffer)]
     (BGFX/bgfx_create_index_buffer ibhMem BGFX/BGFX_BUFFER_NONE))))

(defn memAlloc [size]
  (MemoryUtil/memAlloc (int size)))

(defn memFree [^ByteBuffer buffer]
  (MemoryUtil/memFree buffer))