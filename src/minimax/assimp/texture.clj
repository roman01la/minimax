(ns minimax.assimp.texture
  (:require
   [bgfx.core :as bgfx]
   [clojure.java.io :as io]
   [minimax.mem :as mem]
   [minimax.renderer.texture :as t])
  (:import (java.nio IntBuffer)
           (java.util ArrayList)
           (org.lwjgl.assimp AIScene AITexture)
           (org.lwjgl.bgfx BGFX)
           (org.lwjgl.stb STBImage)))

(set! *warn-on-reflection* true)

(defn process-textures [^AIScene scene]
  (let [textures (ArrayList.)]
    (when-let [m-textures (.mTextures scene)]
      (while (.hasRemaining m-textures)
        (.add textures (AITexture/create (.get m-textures)))))
    textures))

(defn create-image [load handle]
  (mem/slet [^IntBuffer x [:int 1]
             ^IntBuffer y [:int 1]
             ^IntBuffer channels [:int 1]]
    (let [data (load x y channels)
          width (.get x)
          height (.get y)
          channels (.get channels)]
      (handle width height channels data))))

(defn create-texture* [name load]
  (create-image load
                (fn [width height channels data]
                  (let [texture (t/create-2d
                                 {:width width
                                  :height height
                                  :format BGFX/BGFX_TEXTURE_FORMAT_RGBA8
                                  :mem (bgfx/make-ref-release data)})]
                    {:name name
                     :data texture
                     :width width
                     :height height
                     :channels channels}))))

(def dummy-texture
  (delay
    (let [path (.getAbsolutePath (io/file (io/resource "textures/w1x1.png")))]
      (create-texture* "dummy"
                       (fn [^IntBuffer x ^IntBuffer y ^IntBuffer channels]
                         (STBImage/stbi_load path x y channels 4))))))

(defn create-texture [^AITexture texture]
  (let [name (.dataString (.mFilename texture))
        buff (.pcDataCompressed texture)]
    (create-texture* name
                     (fn [^IntBuffer x ^IntBuffer y ^IntBuffer channels]
                       (STBImage/stbi_load_from_memory buff x y channels 4)))))

(defn scene->textures [^AIScene scene]
  (->> (process-textures scene)
       (mapv create-texture)))
