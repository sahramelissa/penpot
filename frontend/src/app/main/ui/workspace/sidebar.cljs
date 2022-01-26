;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) UXBOX Labs SL

(ns app.main.ui.workspace.sidebar
  (:require
   [app.main.refs :as refs]
   [app.main.ui.hooks.resize :refer [use-resize-hook]]
   [app.main.ui.workspace.comments :refer [comments-sidebar]]
   [app.main.ui.workspace.sidebar.assets :refer [assets-toolbox]]
   [app.main.ui.workspace.sidebar.history :refer [history-toolbox]]
   [app.main.ui.workspace.sidebar.layers :refer [layers-toolbox]]
   [app.main.ui.workspace.sidebar.options :refer [options-toolbox]]
   [app.main.ui.workspace.sidebar.sitemap :refer [sitemap]]
   [cuerdas.core :as str]
   [rumext.alpha :as mf]))

;; --- Left Sidebar (Component)

(mf/defc left-sidebar
  {:wrap [mf/memo]}
  [{:keys [layout ] :as props}]
  (let [{:keys [on-pointer-down on-lost-pointer-capture on-mouse-move parent-ref size]}
        (use-resize-hook 255 255 500 :x false :left)]

    [:aside.settings-bar.settings-bar-left {:ref parent-ref
                                            :style #js {"--width" (str size "px")}}
     [:div.resize-area {:on-pointer-down on-pointer-down
                        :on-lost-pointer-capture on-lost-pointer-capture
                        :on-mouse-move on-mouse-move}]
     [:div.settings-bar-inside
      {:data-layout (str/join "," layout)}
      (when (contains? layout :layers)
        [:*
         [:& sitemap {:layout layout}]
         [:& layers-toolbox]])

      (when (contains? layout :document-history)
        [:& history-toolbox])

      (when (contains? layout :assets)
        [:& assets-toolbox])]]))

;; --- Right Sidebar (Component)

(mf/defc right-sidebar
  {::mf/wrap-props false
   ::mf/wrap [mf/memo]}
  [props]
  (let [{:keys [on-pointer-down on-lost-pointer-capture on-mouse-move parent-ref size]}
        (use-resize-hook 255 255 500 :x true :right)

        drawing-tool (:tool (mf/deref refs/workspace-drawing))]
    [:aside.settings-bar.settings-bar-right {:ref parent-ref
                                             :style #js {"--width" (str size "px")}}
     [:div.resize-area {:on-pointer-down on-pointer-down
                        :on-lost-pointer-capture on-lost-pointer-capture
                        :on-mouse-move on-mouse-move}]
     [:div.settings-bar-inside
      (if (= drawing-tool :comments)
        [:& comments-sidebar]
        [:> options-toolbox props])]]))

