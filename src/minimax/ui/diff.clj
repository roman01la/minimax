(ns minimax.ui.diff
  (:require [minimax.state :as ms]))

(def !current-instance (ms/atom nil))

(defprotocol IElement)

(defrecord PrimitiveElement [type props]
  IElement)

(defrecord ComponentElement [type props]
  IElement)

(defn- wrap [v]
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
  (assoc el :return (mapv evaluate (-> el :props :children))))

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
  (swap! !current-instance assoc-in [type name] state))

(defn- get-state [type name]
  (-> @!current-instance type name))

(defn- create-set-state [name]
  (fn [v]
    (if (fn? v)
      (swap! !current-instance update-in [:state name] v)
      (swap! !current-instance assoc-in [:state name] v))))

;;  ==== Public API ====

;; createElement
(defn $ [type props & children]
  (let [props (assoc props :children children)]
    (if (keyword? type)
      (PrimitiveElement. type props)
      (ComponentElement. type props))))

;; Hooks
(defn use-state [name v]
  (when-not (get-state :state name)
    (let [set-state (create-set-state name)]
      (insert-state :state name {:value v :set-state set-state})))
  (let [{:keys [value set-state]} (get-state :state name)]
    [value set-state]))

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
