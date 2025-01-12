(ns minimax.renderer.index-buffer
  (:require [bgfx.core :as bgfx]
            [minimax.mem :as mem])
  (:import (clojure.lang IDeref)
           (java.nio ByteBuffer IntBuffer)
           (java.util ArrayList)
           (org.lwjgl.bgfx BGFX)))

(set! *warn-on-reflection* true)

(defn convert-topology-lines [indices]
  (mem/slet [^IntBuffer dst [:int (count indices)]
             ^IntBuffer source [:int (count indices)]]
    (let [ret (ArrayList.)]
      (doseq [idx indices]
        (.put source ^int idx))
      (.flip source)
      (BGFX/bgfx_topology_convert
       BGFX/BGFX_TOPOLOGY_CONVERT_TRI_LIST_TO_LINE_LIST
       dst source true)
      (while (pos? (.remaining dst))
        (.add ret (.get dst)))
      ret)))

(defn- unsigned-short [x]
  (cond
    (> x 65535)
      (throw (IllegalArgumentException. "Too large for unsigned short"))
    (> x 32767)
     ; make negative
      (short (- x 65536))
    :else
      (short x)))

(defn- create* [^ArrayList indices]
  (let [mem ^ByteBuffer (mem/alloc :byte (* 2 (.size indices)))]
    (doseq [idx indices]
      ;(.putShort mem (short idx)))
      (.putShort mem (unsigned-short idx)))
    (assert (zero? (.remaining mem)) "ByteBuffer size and number of arguments do not match")
    (.flip mem)
    (bgfx/create-index-buffer (bgfx/make-ref-release mem))))

(defrecord IndexBuffer [buffer indices]
  IDeref
  (deref [this]
    buffer))

(defn create
  ([indices]
   (create indices :triangle))
  ([indices primitive-type]
   (let [indices (case primitive-type
                   :line (convert-topology-lines indices)
                   indices)]
     (map->IndexBuffer {:indices indices
                        :buffer (create* indices)}))))
