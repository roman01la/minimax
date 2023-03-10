(ns minimax.ui.components
  (:require
   [fg.clock :as clock]
   [minimax.glfw.core :as glfw]
   [minimax.state :as ms]
   [minimax.ui.diff :as diff :refer [$]]
   [minimax.ui.elements :as ui]
   [minimax.ui.primitives :as ui.pmt])
  (:import (org.lwjgl.glfw GLFW)
           (org.lwjgl.nanovg NanoVG)))

(def !id (ms/atom 0))
(def !current-element (ms/atom nil))
(def !registry (ms/atom {}))

(defn- get-from-registry [path]
  (get-in @!registry (into [@!current-element] path)))

(defn- put-into-registry [path v]
  (swap! !registry assoc-in (into [@!current-element] path) v))

(defn use-state
  ([v]
   (throw (UnsupportedOperationException. "use-state")))
  ([name v]
   (when-not (get-from-registry [:state name])
     (put-into-registry [:state name] (ms/atom (if (fn? v) (v) v))))
   (get-from-registry [:state name])))

(defn use-memo
  ([f deps]
   (throw (UnsupportedOperationException. "use-memo")))
  ([name f deps]
   (when-not (get-from-registry [:memo name])
     (put-into-registry [:memo name] (f)))
   (get-from-registry [:memo name])))

(defn use-callback
  ([f deps]
   (throw (UnsupportedOperationException. "use-callback")))
  ([name f deps]
   (when-not (get-from-registry [:callback name])
     (put-into-registry [:callback name] f))
   (get-from-registry [:callback name])))

(defmacro hlet [bindings & body]
  `(let [~@(->> bindings
                (partition 2)
                (mapcat
                 (fn [[sym [f & args]]]
                   [sym `(~f ~(keyword sym) ~@args)])))]

     ~@body))

(defmacro defui [name args & body]
  `(defn ~name ~args
     (reset! !current-element (str ~(str (ns-name *ns*) "/" name) "::" (swap! !id inc)))
     ~@body))

;; UI components
(defn- scroll-bar [py h height]
  (let [sbw 6
        sbh (- height (- h height))]
    (ui/view
     {:style {:position :absolute
              :top (- py)
              :right 0
              :width sbw
              :height sbh
              :border-radius (/ sbw 2)
              :background-color #ui/rgba [100 100 100 1]}})))

(defui scroll-view [props & children]
  (let [state (use-state :state {:py 0 :h 0 :sh 0 :wh 0})
        height (:sh @state)
        width (:wh @state)
        scroll-bar? (> (:h @state) height)]
    (ui/scroll-view
     (merge
      props
      {:position :relative
       :on-layout (fn [x y w h]
                    (swap! state assoc :sh h :wh w))
       :on-scroll (fn [sx sy]
                    (when scroll-bar?
                      (let [{:keys [py h]} @state
                            npy (+ py (* sy 10))]
                        (swap! state assoc :py
                               (cond
                                 (pos? npy) 0
                                 (neg? (+ npy (- h height))) (- height h)
                                 :else npy)))))})
     (apply ui/view
            {:on-layout (fn [x y w h]
                          (swap! state assoc :h h))
             :style {:top (:py @state)
                     :left 0
                     :position :absolute
                     :width width}}
            children)
     (when scroll-bar?
       (scroll-bar (:py @state) (:h @state) height)))))

(defn view [{:keys [on-mouse-enter on-mouse-leave] :as props}]
  (let [mouse-over? (diff/use-state :mouse-over? false)
        {:keys [cursor]} (:style props)
        props (assoc props
                     :on-mouse-over (fn [hover?]
                                      (when (and (not @mouse-over?) hover?)
                                        (when cursor (glfw/set-cursor cursor))
                                        (when on-mouse-enter (on-mouse-enter)))
                                      (when (and @mouse-over? (not hover?))
                                        (when cursor (glfw/set-cursor :default))
                                        (when on-mouse-leave (on-mouse-leave)))
                                      (reset! mouse-over? hover?)))]
    ($ :view props)))

(defui button [props & children]
  (let [state (use-state :state {:hover? false :pressed? false})
        {:keys [hover? pressed?]} @state
        style (:style props)
        background-color (or (when hover?
                               (if pressed?
                                 (:active/background-color style)
                                 (:hover/background-color style)))
                             (:background-color style))]
    (apply view
           (merge
            props
            {:on-mouse-down #(swap! state assoc :pressed? true)
             :on-mouse-up #(swap! state assoc :pressed? false)
             :on-mouse-enter #(swap! state assoc :hover? true)
             :on-mouse-leave #(swap! state assoc :hover? false)}
            {:style (assoc (:style props) :background-color background-color)})
           children)))

(defui button-text [props text]
  (let [state (use-state :state {:hover? false :pressed? false})
        {:keys [hover? pressed?]} @state
        text-style (:text/style props)
        {:keys [on-mouse-down on-mouse-up]} props
        text-color (or (when hover?
                         (if pressed?
                           (:active/text-color text-style)
                           (:hover/text-color text-style)))
                       (:text-color text-style))]
    (view
     (merge
      props
      {:on-mouse-down (fn []
                        (swap! state assoc :pressed? true)
                        (when on-mouse-down (on-mouse-down)))
       :on-mouse-up (fn []
                      (swap! state assoc :pressed? false)
                      (when on-mouse-up (on-mouse-up)))
       :on-mouse-enter #(swap! state assoc :hover? true)
       :on-mouse-leave #(swap! state assoc :hover? false)})
     (ui/text
      {:style (assoc (:text/style props) :text-color text-color)}
      text))))

(defn widget-header [{:keys [on-mouse-down expanded? title children]}]
  ($ :view {}
     ($ :view
        {:on-mouse-down on-mouse-down
         :style {:align-items :center
                 :flex-direction :row
                 :padding [8 0]
                 :background-color #ui/rgba [50 50 50 1]
                 :border-radius (if expanded? [5 5 0 0] 5)}}
        ($ :text
           {:style {:font-size 12
                    :font-face "IBMPlexMono-Regular"
                    :text-color #ui/rgba [230 230 230 1]
                    :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
           title))
     (when expanded?
       children)))

(defn widget*
  [{:keys [style on-header-click expanded? title
           header children]}]
  ($ view
     {:style style}
     ($ widget-header
        {:on-mouse-down on-header-click
         :expanded? expanded?
         :title title}
        header)
     children))

(defn scroll-widget [{:keys [title width height on-header-click expanded? header]} & children]
  (widget*
   {:style {:width width}
    :on-header-click on-header-click
    :expanded? expanded?
    :title title
    :header header}
   (apply scroll-view
          {:style {:width width
                   :height height
                   :background-color #ui/rgba [35 35 35 1]
                   :border-radius [0 0 5 5]}}
          children)))

(defn widget
  [{:keys [title style on-header-click expanded? children]
    :or {expanded? true}}]
  ($ widget*
     {:style style
      :on-header-click on-header-click
      :expanded? expanded?
      :title title}
     ($ :view
        {:style {:background-color #ui/rgba [35 35 35 1]
                 :border-radius [0 0 5 5]
                 :padding [0 8]}}
        children)))

(def ^:private ks
  [:text-color :text-align :font-size :font-face])

(def measure-text (memoize ui.pmt/measure-text))

(defn use-interval [f it]
  (let [t (use-state :time (clock/time))
        now (clock/time)]
    (when (>= (- now t) it)
      (reset! t now)
      (f))
    @t))

(defui text-input [{:keys [style on-change on-focus on-blur value placeholder]}]
  (let [text-style (select-keys style ks)
        style (apply dissoc style ks)
        state (use-state :state {:focus? false
                                 :cursor-x1 (count value)
                                 :cursor-x2 (count value)
                                 :height 0})
        {:keys [focus? cursor-x1 cursor-x2 height]} @state
        focus-style (when focus?
                      {:border-width 2
                       :border-color #ui/rgba [120 120 255 1]})
        bbs (->> value
                 (map #(nth (measure-text text-style (str %)) 0)))
        x1w (->> bbs
                 (take cursor-x1)
                 (apply +))
        x2w (->> bbs
                 (take cursor-x2)
                 (apply +))

        move-left #(max (dec %) 0)
        move-right #(min (inc %) (count value))
        on-key-down (fn [key mods codepoint]
                      (cond

                        ;; inserted char
                        codepoint
                        (let [ch (char codepoint)
                              value (str
                                     (subs value 0 cursor-x2)
                                     ch
                                     (subs value cursor-x2))]
                          (swap! state update :cursor-x2 inc)
                          (on-change value))

                        :else
                        (condp = key
                          ;; range selection
                          #_#_GLFW/GLFW_KEY_LEFT_SHIFT
                            (swap! state update :cursor-x1 move-left)

                          ;; deletion
                          GLFW/GLFW_KEY_BACKSPACE
                          (when (pos? (count value))
                            (let [value (str
                                         (subs value 0 (dec cursor-x2))
                                         (subs value cursor-x2))]
                              (swap! state update :cursor-x2 dec)
                              (on-change value)))

                          ;; caret movement
                          GLFW/GLFW_KEY_LEFT
                          (doto state
                            (swap! update :cursor-x1 move-left)
                            (swap! update :cursor-x2 move-left))

                          GLFW/GLFW_KEY_RIGHT
                          (doto state
                            (swap! update :cursor-x1 move-right)
                            (swap! update :cursor-x2 move-right))

                          nil)))]
    (ui/view
     {:style (merge style focus-style)
      :on-mouse-down (fn []
                       (when-not focus?
                         (when on-focus (on-focus)))
                       (swap! state assoc :focus? true))
      :on-mouse-down-outside (fn []
                               (when (and focus? on-blur)
                                 (on-blur))
                               (swap! state assoc :focus? false))}
     (ui/view
      {:on-key-down (when focus? on-key-down)
       :on-layout (fn [x y w h]
                    (swap! state assoc :height h))}
      (if (and (= "" value) placeholder (not focus?))
        (ui/text
         {:style (merge text-style {:text-color (:placeholder-color style)})}
         placeholder)
        (ui/text
         {:style text-style}
         value))
      (when focus?
          ;; caret
        (ui/view
         {:style {:width 2
                  :height height
                  :background-color #ui/rgba [200 200 200 0.75]
                  :position :absolute
                  :left x2w
                  :top 0}}))
        ;; selection
      #_(when (not= cursor-x1 cursor-x2)
          (ui/view
           {:style {:width (- x2w x1w)
                    :height height
                    :background-color #ui/rgba [200 200 200 0.3]
                    :position :absolute
                    :left x1w
                    :top 0}}))))))
