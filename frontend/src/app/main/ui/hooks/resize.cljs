;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) UXBOX Labs SL

(ns app.main.ui.hooks.resize
  (:require
   [app.common.geom.point :as gpt]
   [app.common.logging :as log]
   [app.util.dom :as dom]
   [rumext.alpha :as mf]))

(log/set-level! :warn)

(def last-resize-type nil)

(defn set-resize-type! [type]
  (set! last-resize-type type))

(defn use-resize-hook
  [initial min-val max-val axis negate? resize-type]
  (let [size-state (mf/use-state initial)
        parent-ref (mf/use-ref nil)

        dragging-ref (mf/use-ref false)
        start-size-ref (mf/use-ref nil)
        start-ref (mf/use-ref nil)

        on-pointer-down
        (fn [event]
          (dom/capture-pointer event)
          (mf/set-ref-val! start-size-ref @size-state)
          (mf/set-ref-val! dragging-ref true)
          (mf/set-ref-val! start-ref (dom/get-client-position event))
          (set! last-resize-type resize-type))

        on-lost-pointer-capture
        (fn [event]
          (dom/release-pointer event)
          (mf/set-ref-val! start-size-ref nil)
          (mf/set-ref-val! dragging-ref false)
          (mf/set-ref-val! start-ref nil)
          (set! last-resize-type nil))

        on-mouse-move
        (fn [event]
          (when (mf/ref-val dragging-ref)
            (let [start (mf/ref-val start-ref)
                  pos (dom/get-client-position event)
                  delta (-> (gpt/to-vec start pos)
                            (cond-> negate? gpt/negate)
                            (get axis))
                  start-size (mf/ref-val start-size-ref)
                  new-size (-> (+ start-size delta) (max min-val) (min max-val))]
              
              (reset! size-state new-size))))]
    {:on-pointer-down on-pointer-down
     :on-lost-pointer-capture on-lost-pointer-capture
     :on-mouse-move on-mouse-move
     :parent-ref parent-ref
     :size @size-state}))

(defn use-resize-observer
  [callback]

  (let [prev-val-ref (mf/use-ref nil)
        current-observer-ref (mf/use-ref nil)

        node-ref
        (mf/use-callback
         (mf/deps callback)
         (fn [node]
           (let [current-observer (mf/ref-val current-observer-ref)
                 prev-val         (mf/ref-val prev-val-ref)]

             (when (and (not= prev-val node) (some? current-observer))
               (log/debug :action "disconnect" :js/prev-val prev-val :js/node node)
               (.disconnect current-observer)
               (mf/set-ref-val! current-observer-ref nil))

             (when (and (not= prev-val node) (some? node))
               (let [observer
                     (js/ResizeObserver.
                      (fn []
                        (let [size (dom/get-client-size node)]
                          (when callback (callback last-resize-type size)))))]
                 (mf/set-ref-val! current-observer-ref observer)
                 (log/debug :action "observe"  :js/node node)
                 (.observe observer node))))
           (mf/set-ref-val! prev-val-ref node)))]

    (mf/use-effect
     (fn []
       (fn []
         (let [current-observer (mf/ref-val current-observer-ref)]
           (when (some? current-observer)
             (log/debug :action "disconnect")
             (.disconnect current-observer))))))
    node-ref))
