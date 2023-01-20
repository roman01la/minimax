(ns minimax.util.fs
  (:require [clojure.java.io :as io]
            [minimax.mem :as mem])
  (:import (java.io File)
           (java.nio ByteBuffer)))

(set! *warn-on-reflection* true)

(defn ^ByteBuffer load-resource [^File f]
  (with-open [is (io/input-stream f)]
    (let [bytes ^ByteBuffer (mem/alloc :byte (.length f))]
      (.put bytes (.readAllBytes is))
      (.flip bytes)
      bytes)))
