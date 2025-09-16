(ns fg.dev
  (:require [clojure.java.io :as io]
            [minimax.logger :as log]
            [nrepl.server])
  (:import (java.nio.file FileSystems Path StandardWatchEventKinds WatchEvent WatchService)))

(def ^WatchService watch-service
  (.newWatchService (FileSystems/getDefault)))

(defn register! [path]
  (.register path
             watch-service
             (into-array
              [StandardWatchEventKinds/ENTRY_CREATE
               StandardWatchEventKinds/ENTRY_MODIFY
               StandardWatchEventKinds/ENTRY_DELETE])))

(def watcher (atom nil))
(def change-listeners (atom {}))

(defn start []
  (nrepl.server/start-server :port 7888)
  (println "nrepl server started on port 7888")
  (reset! watcher
          (doto (Thread.
                 (fn []
                   (log/debug "shader watcher has started")
                   (try
                     (loop []
                       (let [key (.take watch-service)]
                         (doseq [^WatchEvent event (.pollEvents key)]
                           (let [path (.toString (.toAbsolutePath ^Path (.context event)))]
                             (when-let [entry (get @change-listeners path)]
                               (let [[cmd on-change] entry]
                                 (if (zero? (cmd path))
                                   (do (log/debug "Recompiled shaders")
                                       (on-change))
                                   (log/debug "Failed to recompile shaders"))))))
                         (when (.reset key)
                           (recur))))
                     (catch InterruptedException e
                       (log/debug "shader watcher has stopped")))))
            (.start))))

(defn watch [res cmd on-change]
  (let [path (.toString (.toPath (io/file res)))
        dir (.toPath (.getParentFile (io/file res)))]
    (swap! change-listeners assoc path [cmd on-change])
    (register! dir)))

(defn stop []
  (when-let [t @watcher]
    (.interrupt ^Thread t)
    (.close watch-service)
    (reset! watcher nil)))
