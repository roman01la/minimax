(ns minimax.ui.elements2
  (:require [minimax.ui.diff :as diff]
            [minimax.ui.elements :as ui])
  (:import (minimax.ui.diff PrimitiveElement)))

(def primitive-elements
  {:view ui/view
   :scroll-view ui/scroll-view
   :text ui/text
   :image ui/image
   :root ui/root})

(defn get-layout [node]
  (cond
    (instance? PrimitiveElement node)
    (let [f (primitive-elements (:type node))
          props (dissoc (:props node) :children)
          children (mapcat (comp diff/wrap get-layout) (:return node))]
      (apply f props children))

    (map? node)
    (mapcat (comp diff/wrap get-layout) (:return node))

    :else node))

