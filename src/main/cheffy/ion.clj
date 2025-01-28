(ns cheffy.ion
  (:require [com.stuartsierra.component :as component]
            [cheffy.components.auth :as auth]
            [cheffy.components.database :as database]
            [cheffy.routes :as routes]
            [cheffy.components.api-server :as api-server]
            [io.pedestal.ions :as pi]
            [io.pedestal.http :as http]
            [datomic.ion :as ion]))


(def config
  {:database {:server-type :ion
              :system "cheffy-prod"
              :db-name "cheffy-prod"
              :region ""
              :endpoint ""}
   :auth {:client-id ""
          :client-secret (get (ion/get-params {:path "/datomic-shared/prod/cheffy/"}) "cognito-client-secret")
          :user-pool-id ""
          :jwks ""}})

(defn create-provide
  [config]
  (let [system
        (component/system-map
         :config config
         :auth (auth/service (:auth config))
         :database (database/service (:database config)))

        {:keys [database auth]}
        (component/start system)

        provider
        (-> {:env :prod
             ::http/routes (routes/routes)
             ::http/chain-provider pi/ion-provider}
            (api-server/cheffy-interceptors [(api-server/inject-system {:system/database database :system/auth auth})])
            (http/create-provider))]
    provider))

(def app
  (delay
    (create-provide config)))

(defn handler
  [req]
  (@app req))