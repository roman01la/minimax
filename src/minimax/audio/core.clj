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

(defn add [name file]
  (audio.ctx/add-source @!context name file))

(defn remove [name]
  (audio.ctx/remove-source @!context name))

(defn play [name]
  (audio.ctx/play-source @!context name))

(defn pause [name]
  (audio.ctx/pause-source @!context name))

(defn stop [name]
  (audio.ctx/stop-source @!context name))

(defn rewind [name]
  (audio.ctx/rewind-source @!context name))

(defn set-gain [value]
  (audio.ctx/set-gain @!context value))

(defn shutdown []
  (audio.ctx/destroy @!context))
