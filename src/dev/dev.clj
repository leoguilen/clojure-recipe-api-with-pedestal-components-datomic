(ns dev
  (:require
   [cheffy.server :as server]
   [clojure.edn :as edn]
   [com.stuartsierra.component.repl :as cr]
   [datomic.client.api :as d]
   [io.pedestal.http :as http]
   [io.pedestal.test :as pt]))

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

  (pt/response-for
   (-> cr/system :api-server :service ::http/service-fn)
   :get
   "/recipes")

  (d/q '[:find ?e ?id
         :where [?e :account/account-id ?id]]
       (d/db (-> cr/system :database :conn)))

  (start-dev)
  (stop-dev)
  (restart-dev)
  (-> cr/system :api-server :service ::http/service-fn)

  (let [conn (-> cr/system :database :conn)
        db (d/db conn)]
    (contains? (d/pull db {:eid :account/account-id :selector '[*]}) :db/ident))
  )