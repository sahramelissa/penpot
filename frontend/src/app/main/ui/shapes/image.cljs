;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) UXBOX Labs SL

(ns app.main.ui.shapes.image
  (:require
   [app.common.geom.shapes :as gsh]
   [app.config :as cfg]
   [app.main.ui.context :as muc]
   [app.main.ui.shapes.attrs :as attrs]
   [app.main.ui.shapes.custom-stroke :refer [shape-custom-stroke]]
   [app.main.ui.shapes.embed :as embed]
   [app.util.object :as obj]
   [rumext.alpha :as mf]))

(mf/defc image-shape
  {::mf/wrap-props false}
  [props]

  (let [shape (unchecked-get props "shape")
        {:keys [x y width height metadata]} shape
        uri   (cfg/resolve-file-media metadata)
        embed (embed/use-data-uris [uri])

        render-id  (mf/use-ctx muc/render-ctx)
        filter-id (str "image-" render-id)

        filter (str "url(#" filter-id ")")
        transform (gsh/transform-matrix shape)
        props (-> (attrs/extract-style-attrs shape)
                  (obj/merge!
                   #js {:x x
                        :y y
                        :transform transform
                        :width width
                        :height height
                        :preserveAspectRatio "none"
                        :data-loading (str (not (contains? embed uri)))
                        ;; :filter filter
                        }))

        stroke-width (:stroke-width shape 0)
        margin (gsh/shape-stroke-margin shape stroke-width)]

    [:g
     [:defs
      [:filter {:id filter-id
                :x (- x (+ stroke-width margin))
                :y (- y (+ stroke-width margin))
                :width (+ width (* 2 (+ stroke-width margin)))
                :height (+ height (* 2 (+ stroke-width margin)))
                :filterUnits "userSpaceOnUse"}
       [:feImage {:xlinkHref (get embed uri uri)
                  :x x
                  :y y
                  :width width
                  :height height
                  :preserveAspectRatio "none"}]
       [:feComposite {:in2 "SourceGraphic" :operator "over" :result "fill-area"}]]]
     [:& shape-custom-stroke {:shape shape :filter filter}
      [:> :rect props]]]))
