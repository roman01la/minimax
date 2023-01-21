(ns minimax.audio.core
  (:require [clojure.java.io :as io]
            [minimax.audio.context :as audio.ctx]
            [minimax.logger :as log]))

(def !context (atom nil))

(defn init []
  (log/debug "Loading sounds...")
  (let [context (audio.ctx/create)]
    (time
      (doseq [[name file] [[:bg (io/file (io/resource "sounds/bg.ogg"))]]]
        (audio.ctx/add-source context name file)))
    (reset! !context context)))

(defn play [name]
  (audio.ctx/play-source @!context name))

(defn shutdown []
  (audio.ctx/destroy @!context))
