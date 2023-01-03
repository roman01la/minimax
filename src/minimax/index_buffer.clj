(ns minimax.index-buffer
  (:require [bgfx.core :as bgfx])
  (:import (clojure.lang IDeref)
           (java.util ArrayList)
           (org.lwjgl.system MemoryUtil)))

(set! *warn-on-reflection* true)

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

(defn create [indices]
  (map->IndexBuffer {:indices indices
                     :buffer (create* indices)}))
