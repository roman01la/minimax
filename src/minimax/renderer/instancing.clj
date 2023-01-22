(ns minimax.renderer.instancing
  (:require [bgfx.core :as bgfx]
            [minimax.mem :as mem])
  (:import (org.lwjgl.bgfx BGFX BGFXInstanceDataBuffer)
           (org.lwjgl.system MemoryStack)))

(def instancing-supported?
  (delay (not= 0 (bit-and BGFX/BGFX_CAPS_INSTANCING (.supported (bgfx/get-caps))))))

;; 4x4 float matrix = 64 + 16 RGBA float = 80 bytes
(def ^:private instance-stride 80)

(defn render [num-instances]
  (mem/slet [idb BGFXInstanceDataBuffer]
    (let [instances-available (BGFX/bgfx_get_avail_instance_data_buffer num-instances instance-stride)
          _ (BGFX/bgfx_alloc_instance_data_buffer idb num-instances instance-stride)
          data (.data idb)]
      (loop [idx 0]
        (when (< idx instances-available)
          (recur (inc idx))))
      (BGFX/bgfx_set_instance_data_buffer idb 0 num-instances)
      (MemoryStack/stackPop))))
