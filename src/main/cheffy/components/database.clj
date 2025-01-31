(ns cheffy.components.database
  (:require [com.stuartsierra.component :as component]
            [datomic.client.api :as d]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn ident-has-attr?
  [db ident attr]
  (contains? (d/pull db {:eid ident :selector '[*]}) attr))

(defn load-dataset
  [conn]
  (let [db (d/db conn)
        tx #(d/transact conn {:tx-data %})]
    (when-not (ident-has-attr? db :account/account-id :db/ident)
      (tx (-> (io/resource "cheffy/schema.edn") (slurp) (edn/read-string)))
      (tx (-> (io/resource "cheffy/seed.edn") (slurp) (edn/read-string))))))

(defn ion?
  [config]
  (= :ion (:server-type config)))

(defrecord Database [config conn]

  component/Lifecycle

  (start [component]
    (println ";; Starting Database")
    (let [db-name (select-keys config [:db-name])
          client (d/client (if (ion? config)
                             (select-keys config [:server-type :system :region :endpoint])
                             (select-keys config [:server-type :storage-dir :system])))
          _ (d/create-database client db-name)
          conn (d/connect client db-name)]
      (load-dataset conn)
      (assoc component :conn conn)))

  (stop [component]
    (println ";; Stopping Database")
    (assoc component :conn nil)))

(defn service
  [config]
  (map->Database {:config config}))