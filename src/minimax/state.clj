(ns minimax.state
  "Lightweight Atom, like Volatile, but with watchers"
  (:refer-clojure :exclude [atom])
  (:import (clojure.lang IAtom IDeref IRef)))

(defn- notify-watches [ref watches oldv newv]
  (doseq [[key f] watches]
    (f key ref oldv newv)))

(deftype State [^:volatile-mutable value ^:volatile-mutable watches]
  IDeref
  (deref [this]
    value)
  IAtom
  (reset [this v]
    (let [oldv value]
      (set! value v)
      (notify-watches this watches oldv value)
      value))
  (swap [this f]
    (reset! this (f value)))
  (swap [this f arg1]
    (reset! this (f value arg1)))
  (swap [this f arg1 arg2]
    (reset! this (f value arg1 arg2)))
  (swap [this f arg1 arg2 args]
    (reset! this (apply f value arg1 arg2 args)))
  IRef
  (addWatch [this key f]
    (set! watches (assoc watches key f))
    this)
  (removeWatch [this key]
    (set! watches (dissoc watches key))
    this)
  (getWatches [this]
    watches))

(defn atom [v]
  (State. v {}))
