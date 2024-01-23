;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.srepl.cli
  "PREPL API for external usage (CLI or ADMIN)"
  (:require
   [app.auth :as auth]
   [app.common.exceptions :as ex]
   [app.common.uuid :as uuid]
   [app.db :as db]
   [app.main :as main]
   [app.rpc.commands.auth :as cmd.auth]
   [app.srepl.components-v2 :refer [migrate-teams!]]
   [app.util.events :as events]
   [app.util.json :as json]
   [app.util.time :as dt]
   [cuerdas.core :as str]))

(defn- get-current-system
  []
  (or (deref (requiring-resolve 'app.main/system))
      (deref (requiring-resolve 'user/system))))

(defmulti ^:private exec-command ::cmd)

(defn exec
  "Entry point with external tools integrations that uses PREPL
  interface for interacting with running penpot backend."
  [data]
  (let [data (json/decode data)]
    (-> {::cmd (keyword (:cmd data "default"))}
        (merge (:params data))
        (exec-command))))

(defmethod exec-command :create-profile
  [{:keys [fullname email password is-active]
    :or {is-active true}}]
  (when-let [system (get-current-system)]
    (db/with-atomic [conn (:app.db/pool system)]
      (let [params  {:id (uuid/next)
                     :email email
                     :fullname fullname
                     :is-active is-active
                     :password password
                     :props {}}]
        (->> (cmd.auth/create-profile! conn params)
             (cmd.auth/create-profile-rels! conn))))))

(defmethod exec-command :update-profile
  [{:keys [fullname email password is-active]}]
  (when-let [system (get-current-system)]
    (db/with-atomic [conn (:app.db/pool system)]
      (let [params (cond-> {}
                     (some? fullname)
                     (assoc :fullname fullname)

                     (some? password)
                     (assoc :password (auth/derive-password password))

                     (some? is-active)
                     (assoc :is-active is-active))]
        (when (seq params)
          (let [res (db/update! conn :profile
                                params
                                {:email email
                                 :deleted-at nil})]
            (pos? (db/get-update-count res))))))))

(defmethod exec-command :delete-profile
  [{:keys [email soft]}]
  (when-not email
    (ex/raise :type :assertion
              :code :invalid-arguments
              :hint "email should be provided"))

  (when-let [system (get-current-system)]
    (db/with-atomic [conn (:app.db/pool system)]

      (let [res (if soft
                  (db/update! conn :profile
                              {:deleted-at (dt/now)}
                              {:email email :deleted-at nil})
                  (db/delete! conn :profile
                              {:email email}))]
        (pos? (db/get-update-count res))))))

(defmethod exec-command :search-profile
  [{:keys [email]}]
  (when-not email
    (ex/raise :type :assertion
              :code :invalid-arguments
              :hint "email should be provided"))

  (when-let [system (get-current-system)]
    (db/with-atomic [conn (:app.db/pool system)]

      (let [sql (str "select email, fullname, created_at, deleted_at from profile "
                     " where email similar to ? order by created_at desc limit 100")]
        (db/exec! conn [sql email])))))

(defmethod exec-command :derive-password
  [{:keys [password]}]
  (auth/derive-password password))

(defmethod exec-command :migrate-v2
  [_]
  (letfn [(on-progress-report [{:keys [elapsed completed errors]}]
            (println (str/ffmt "-> Progress: completed: %, errors: %, elapsed: %"
                               completed errors elapsed)))

          (on-progress [{:keys [op name]}]
            (case op
              :migrate-team
              (println (str/ffmt "-> Migrating team: \"%\"" name))
              :migrate-file
              (println (str/ffmt "=> Migrating file: \"%\"" name))
              nil))

          (on-event [[type payload]]
            (case type
              :progress-report (on-progress-report payload)
              :progress        (on-progress payload)
              :error           (on-error payload)
              nil))

          (on-error [cause]
            (println "EE:" (ex-message cause)))]

    (println "The components/v2 migration started...")

    (try
      (let [result (-> (partial migrate-teams! main/system {:rollback? true})
                       (events/run-with! on-event))]
        (println (str/ffmt "Migration process finished (elapsed: %)" (:elapsed result))))
      (catch Throwable cause
        (on-error cause)))))

(defmethod exec-command :default
  [{:keys [::cmd]}]
  (ex/raise :type :internal
            :code :not-implemented
            :hint (str/ffmt "command '%' not implemented" (name cmd))))
