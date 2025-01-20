(ns dev
  (:require
   [clojure.edn :as edn]
   [cheffy.server :as server]
   [com.stuartsierra.component.repl :as cr]
   [datomic.client.api :as d]))

(defn system
  [_]
  (-> (-> "src/config/cheffy/development.edn" slurp edn/read-string)
      (server/create-system)))

(cr/set-init system)

(defn start-dev
  []
  (cr/start))

(defn stop-dev
  []
  (cr/stop))

(defn restart-dev
  []
  (cr/reset))

(comment
  (d/q '[:find ?e ?id
         :where [?e :account/account-id ?id]]
       (d/db (-> cr/system :database :conn)))

  (start-dev)
  (stop-dev)
  (restart-dev)
  (-> cr/system :api-server :service)

  (let [conn (-> cr/system :database :conn)
        db (d/db conn)]
    (contains? (d/pull db {:eid :account/account-id :selector '[*]}) :db/ident))
  )