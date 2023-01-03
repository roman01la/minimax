(ns minimax.objects.camera
  (:require [minimax.lib :as lib]
            [minimax.object :as obj])
  (:import (org.joml Matrix4f)))

(set! *warn-on-reflection* true)

(defprotocol ICamera
  (look-at [this at eye]))

(defrecord PerspectiveCamera [proj-mtx view-mtx fov aspect near far]
  ICamera
  (look-at [this at eye]
    (lib/look-at at eye view-mtx)
    (lib/perspective fov aspect near far proj-mtx))
  obj/IRenderable
  (render [this id]
    (lib/set-view-transform id view-mtx proj-mtx)))

(defn create-perspective-camera
  [{:keys [fov aspect near far]}]
  (map->PerspectiveCamera
    {:proj-mtx (Matrix4f.)
     :view-mtx (Matrix4f.)
     :fov fov
     :aspect aspect
     :near near
     :far far}))

(defrecord OrthographicCamera [proj-mtx view-mtx area near far]
  ICamera
  (look-at [this at eye]
    (reset! proj-mtx (Matrix4f.))
    (lib/look-at at eye view-mtx)
    (lib/ortho area near far @proj-mtx))
  obj/IRenderable
  (render [this id]
    (lib/set-view-transform id view-mtx @proj-mtx)))

(defn create-orthographic-camera
  [{:keys [area near far]}]
  (map->OrthographicCamera
    {:proj-mtx (atom (Matrix4f.))
     :view-mtx (Matrix4f.)
     :area area
     :near near
     :far far}))

(defrecord ScreenCamera []
  obj/IRenderable
  (render [this id]
    (let [proj-mtx (Matrix4f.)]
      (lib/ortho-screen proj-mtx)
      (lib/set-view-transform id nil proj-mtx))))

(defn create-screen-camera []
  (map->ScreenCamera {}))

