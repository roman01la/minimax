(ns minimax.index-buffer
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref)
           (java.util ArrayList)
           (org.lwjgl.bgfx BGFX)
           (org.lwjgl.system MemoryUtil)))

(set! *warn-on-reflection* true)

(defn convert-topology-lines [indices]
  (let [ret (ArrayList.)
        dst (MemoryUtil/memAllocInt (count indices))
        source (MemoryUtil/memAllocInt (count indices))]
    (doseq [idx indices]
      (.put source ^int idx))
    (.flip source)
    (BGFX/bgfx_topology_convert
      BGFX/BGFX_TOPOLOGY_CONVERT_TRI_LIST_TO_LINE_LIST
      dst source true)
    (while (pos? (.remaining dst))
      (.add ret (.get dst)))
    ret))

(defn- create* [^ArrayList indices]
  (let [mem (MemoryUtil/memAlloc (* 2 (.size indices)))]
    (doseq [idx indices]
      (.putShort mem (short idx)))
    (assert (zero? (.remaining mem)) "ByteBuffer size and number of arguments do not match")
    (.flip mem)
    (bgfx/create-index-buffer (bgfx/make-ref mem))))

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
