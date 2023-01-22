(ns fg.material
  (:require [clojure.string :as str]
            [fg.passes.shadow :as pass.shadow]
            [fg.shader :as sd]
            [minimax.assimp.texture :as t]
            [minimax.mem :as mem]
            [minimax.renderer.uniform :as u])
  (:import (java.nio IntBuffer)
           (org.joml Vector4f)
           (org.lwjgl.assimp AIColor4D AIMaterial AIScene AIString Assimp)
           (org.lwjgl.bgfx BGFX)))

(set! *warn-on-reflection* true)

(def default-color
  (Vector4f. 0.4 0.4 0.4 1.0))

(defn get-material-color [^AIMaterial material ^CharSequence key]
  (let [color (AIColor4D/create)]
    (if (zero? (Assimp/aiGetMaterialColor material key Assimp/aiTextureType_NONE 0 color))
      (Vector4f. (.r color) (.g color) (.b color) (.a color))
      default-color)))

(defn get-material-texture [^AIMaterial material type textures]
  (let [path (AIString/calloc)
        _ (Assimp/aiGetMaterialTexture
           material
           ^int type
           0
           ^AIString path
           ^IntBuffer (mem/alloc :int 1)
           nil nil nil nil nil)
        filename (.dataString path)]
    (if (= filename "")
      @t/dummy-texture
      (do
        (assert (str/starts-with? filename "*"))
        (let [idx (Integer/parseInt (subs filename 1) 10)]
          (nth textures idx))))))

;; uniforms
(def u-color
  (delay (u/create "u_color" BGFX/BGFX_UNIFORM_TYPE_VEC4)))

(def u-tex-diffuse
  (delay (u/create "s_texDiffuse" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(def u-shadow
  (delay (u/create "s_shadowMap" BGFX/BGFX_UNIFORM_TYPE_SAMPLER)))

(def u-light-mtx
  (delay (u/create "u_light_mtx" BGFX/BGFX_UNIFORM_TYPE_MAT4)))

;; shaders
(def diffuse-shader
  (sd/load-program "fs_geometry" "vs_geometry"))

(def shadow-shader
  (sd/load-program "fs_shadow" "vs_shadow"))

(def line-shader
  (sd/load-program "fs_line" "vs_line"))

;; materials
(defprotocol IMaterial
  (update-uniforms [this light-mtx]))

(defrecord StandardMaterial [ambient diffuse texture shader shadow-shader uniforms]
  IMaterial
  (update-uniforms [this light-mtx]
    (u/set-value (:color uniforms) diffuse)
    (u/set-value (:light-mtx uniforms) light-mtx)
    (u/set-texture (:shadow uniforms) pass.shadow/shadow-map-texture 4)
    (u/set-texture (:texture uniforms) (:data texture) 1)))

(defn create-standard-material
  [{:keys [ambient diffuse texture shader shadow-shader uniforms]}]
  (map->StandardMaterial
   {:ambient ambient
    :diffuse diffuse
    :texture texture
    :shader shader
    :shadow-shader shadow-shader
    :uniforms uniforms}))

(defrecord LineMaterial [diffuse shader uniforms]
  IMaterial
  (update-uniforms [this light-mtx]
    (u/set-value (:color uniforms) diffuse)))

(defn create-line-material [{:keys [color]}]
  (map->LineMaterial
   {:diffuse color
    :shader @line-shader
    :uniforms {:color @u-color}}))

(defn create-material [^AIMaterial material textures]
  (let [ambient (get-material-color material Assimp/AI_MATKEY_COLOR_AMBIENT)
        diffuse (get-material-color material Assimp/AI_MATKEY_COLOR_DIFFUSE)
        texture (get-material-texture material Assimp/aiTextureType_DIFFUSE textures)]
    (create-standard-material
     {:ambient ambient
      :diffuse diffuse
      :texture texture
      :shader @diffuse-shader
      :shadow-shader @shadow-shader
      :uniforms {:color @u-color
                 :texture @u-tex-diffuse
                 :shadow @u-shadow
                 :light-mtx @u-light-mtx}})))

(defn create-materials [^AIScene scene textures]
  (let [materials (.mMaterials scene)]
    (->> (range (.mNumMaterials scene))
         (mapv #(-> (.get materials ^int %) AIMaterial/create (create-material textures))))))
