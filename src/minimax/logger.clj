(ns minimax.logger
  (:refer-clojure :exclude [time]))

(def debug? (System/getenv "MINIMAX_DEBUG"))

(defn debug* [file line column & args]
  (let [file-str (str file ":" line ":" column)]
    (apply println "[MINIMAX]" file-str args)))

(defmacro debug [& args]
  (when debug?
    (let [{:keys [line column]} (meta &form)]
      `(debug* ~*file* ~line ~column ~@args))))

(defmacro time [& body]
  (when debug?
    (let [{:keys [line column]} (meta &form)]
      `(let [s# (new java.io.StringWriter)
             ret# (binding [*out* s#]
                    (clojure.core/time (do ~@body)))]
         (debug* ~*file* ~line ~column (str s#))
         ret#))))
