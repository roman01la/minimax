(ns minimax.renderer.instancing
  (:require [bgfx.core :as bgfx]
            [minimax.mem :as mem])
  (:import (java.nio ByteBuffer)
           (org.joml Matrix4f)
           (org.lwjgl.bgfx BGFX BGFXInstanceDataBuffer)
           (org.lwjgl.system MemoryStack)))

(def instancing-supported?
  (delay (not= 0 (bit-and BGFX/BGFX_CAPS_INSTANCING (.supported (bgfx/get-caps))))))

;; 4x4 float matrix = 64 bytes
(def ^:private instance-stride
  (* 4 4 4))

(defn mtx4->buff [^ByteBuffer buff ^Matrix4f mtx]
  (.putFloat buff (.m00 mtx))
  (.putFloat buff (.m01 mtx))
  (.putFloat buff (.m02 mtx))
  (.putFloat buff (.m03 mtx))

  (.putFloat buff (.m10 mtx))
  (.putFloat buff (.m11 mtx))
  (.putFloat buff (.m12 mtx))
  (.putFloat buff (.m13 mtx))

  (.putFloat buff (.m20 mtx))
  (.putFloat buff (.m21 mtx))
  (.putFloat buff (.m22 mtx))
  (.putFloat buff (.m23 mtx))

  (.putFloat buff (.m30 mtx))
  (.putFloat buff (.m31 mtx))
  (.putFloat buff (.m32 mtx))
  (.putFloat buff (.m33 mtx)))

(defn set-instance-data-buffer [inst-data]
  (mem/slet [idb BGFXInstanceDataBuffer]
    (let [num-instances (count inst-data)
          instances-available (BGFX/bgfx_get_avail_instance_data_buffer num-instances instance-stride)
          _ (BGFX/bgfx_alloc_instance_data_buffer idb num-instances instance-stride)
          data (.data idb)]
      (loop [idx 0]
        (when (< idx instances-available)
          (mtx4->buff data (nth inst-data idx))
          (recur (inc idx))))
      (.flip data)
      (BGFX/bgfx_set_instance_data_buffer idb 0 num-instances))))
