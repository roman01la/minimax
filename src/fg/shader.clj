(ns fg.shader
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [fg.dev]
   [minimax.renderer.program :as program])
  (:import (clojure.lang IDeref)))

(set! *warn-on-reflection* true)

(defn- watch [res on-change]
  (fg.dev/watch res (fn [_] (:exit (sh/sh "./scripts/shaders"))) on-change))

(defn load-program [fs-path vs-path]
  (let [v (atom nil)
        load-program #(reset! v (program/create fs-path vs-path))]
    (watch (io/resource (str "shaders/" fs-path ".sc")) load-program)
    (watch (io/resource (str "shaders/" vs-path ".sc")) load-program)
    (reify IDeref
      (deref [this]
        (if-let [p @v]
          @p
          @(load-program))))))
