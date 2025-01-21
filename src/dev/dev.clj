(ns dev
  (:require
   [cheffy.server :as server]
   [clojure.edn :as edn]
   [cognitect.transit :as transit]
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

(defn- transit-write
  [obj]
  (let [out (java.io.ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer obj)
    (.toString out)))

(defn- transit-read
  [txt]
  (let [in (java.io.ByteArrayInputStream. (.getBytes txt))
        reader (transit/reader in :json)]
    (transit/read reader)))

(comment
  (-> cr/system :api-server :service :env)

  (-> (transit-write {:name "name"
                      :public true
                      :prep-time 30
                      :img "img"})
      (transit-read))

  (pt/response-for
   (-> cr/system :api-server :service ::http/service-fn)
   :put "/recipes/68f7d398-f225-4ab2-8e9f-a723f1654b4b"
   :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
             "Content-Type" "application/transit+json"}
   :body (transit-write {:name "new name"
                         :public true
                         :prep-time 45
                         :img "new img"}))

  (pt/response-for
   (-> cr/system :api-server :service ::http/service-fn)
   :post "/recipes"
   :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
             "Content-Type" "application/transit+json"
             "Accept" "application/transit+json"}
   :body (transit-write {:name "name"
                         :public true
                         :prep-time 30
                         :img "img"}))

  (pt/response-for
   (-> cr/system :api-server :service ::http/service-fn)
   :get "/recipes/68f7d398-f225-4ab2-8e9f-a723f1654b4b"
   :headers {"Accept" "application/transit+json"})

  (pt/response-for
   (-> cr/system :api-server :service ::http/service-fn)
   :get "/recipes"
   :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})

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