(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'minimax/farm)
(def version (format "0.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean []
  (println "Cleaning target dir...")
  (b/delete {:path "target"}))

(defn -main [& args]
  (clean)
  (println "Copying resources...")
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (println "Compiling sources...")
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (println "Packaging an uberjar...")
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'fg.core})
  (println "Done."))
