;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.geom.path
  (:require [uxbox.util.geom.path-impl-simplify :as impl-simplify]))

(defn simplify
  [points]
  (let [points (into-array points)]
    (into [] (impl-simplify/simplify points 10 true))))
