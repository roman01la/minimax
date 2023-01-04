(ns minimax.util.fs
  (:require [clojure.java.io :as io])
  (:import (java.io File)
           (java.nio DirectByteBuffer)
           (org.lwjgl.system MemoryUtil)))

(defn ^DirectByteBuffer load-resource [^File f]
  (with-open [is (io/input-stream f)]
    (let [bytes (MemoryUtil/memAlloc (.length f))]
      (doseq [b (.readAllBytes is)]
        (.put bytes ^byte b))
      (.flip bytes)
      bytes)))
