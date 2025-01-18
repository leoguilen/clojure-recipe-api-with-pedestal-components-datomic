(ns cheffy.server
  (:require
   [cheffy.components.api-server :as api-server]
   [cheffy.components.database :as database]
   [clojure.edn :as edn]
   [com.stuartsierra.component :as component]))

(defn create-system
  [config]
  (component/system-map
   :config config
   :databse (database/service (:database config))
   :api-server (api-server/service (:service-map config))))

(defn -main
  [config-file]
  (let [config (-> config-file (slurp) (edn/read-string))]
    (component/start (create-system config))))