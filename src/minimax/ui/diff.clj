(ns minimax.ui.diff
  (:require [minimax.state :as ms]))

(def ^:private !current-instance (ms/atom nil))

(defprotocol IElement)

(defrecord PrimitiveElement [type props]
  IElement)

(defrecord ComponentElement [type props]
  IElement)

(defn wrap [v]
  (if (sequential? v) v [v]))

(defn- evaluate-once [el]
  (cond
    (instance? ComponentElement el)
    (let [{:keys [type props]} el]
      (assoc el :return (wrap (type props))))

    (instance? PrimitiveElement el)
    (assoc el :return (-> el :props :children))

    (seq? el)
    (vec el)

    :else el))

(defn- with-current-instance [value f]
  (let [_ (reset! !current-instance value)
        ret (f)
        v @!current-instance
        _ (reset! !current-instance nil)]
    [ret v]))

(declare evaluate)

(defn- reconcile-component-element [el1 el2]
  (let [[el2 state] (with-current-instance (:state el1) #(evaluate-once el2))]
    (assoc el2 :state state)))

(defn- reconcile-elements [el1 el2]
  (-> (if (= (:type el1) (:type el2))
        (if (and (instance? ComponentElement el1) (instance? ComponentElement el2))
          (reconcile-component-element el1 el2)
          (evaluate-once el2))
        (evaluate-once el2))
      (update :return #(mapv evaluate (:return el1) %))))

(defn- evaluate-component-element [{:keys [type props] :as el}]
  (let [[v state] (with-current-instance {} #(type props))]
    (assoc el :return (mapv evaluate (wrap v))
           :state state)))

(defn- reconcile-primitive-element [el]
  (->> el :props :children (mapv evaluate) (assoc el :return)))

(defn- evaluate
  ([el]
   (cond
     (instance? ComponentElement el)
     (evaluate-component-element el)

     (instance? PrimitiveElement el)
     (reconcile-primitive-element el)

     (seq? el)
     (mapv evaluate el)

     :else el))
  ([el1 el2]
   (if (and (satisfies? IElement el1) (satisfies? IElement el2))
     (reconcile-elements el1 el2)
     (evaluate el2))))

;; Hooks
(defn- insert-state [type name state]
  (swap! !current-instance assoc-in [type name] state)
  state)

(defn- get-state [type name]
  (-> @!current-instance type name))

;;  ==== Public API ====

;; createElement
(defn $ [type & args]
  (let [props (if (and (map? (first args))
                       (not (record? (first args))))
                (-> (first args)
                    (assoc :children (rest args)))
                {:children args})]
    (if (keyword? type)
      (PrimitiveElement. type props)
      (ComponentElement. type props))))

;; Hooks
(defn use-state [name v]
  (or (get-state :state name)
      ;; TODO: immutable state
      (insert-state :state name (ms/atom v))))

;; Render loop
(defn- render-root [root el]
  (reset! root
          (if-let [pel @root]
            (evaluate pel el)
            (evaluate el))))

;; Top-level
(defn create-root []
  (ms/atom nil))

(defn render [root el]
  (render-root root el))
