(ns cheffy.components.api-server 
  (:require
    [com.stuartsierra.component :as component]
    [cheffy.routes :as routes]
    [io.pedestal.http :as http]))

(defrecord ApiServer [service-map service]
  
  component/Lifecycle
  
  (start [component]
    (println ";; Starting API server")
    (let [service (-> service-map
                      (assoc ::http/routes routes/routes)
                      (http/create-server)
                      (http/start))]
      (assoc component :service service)))
  
  (stop [component]
    (println ";; Stopping API server")
    (when service
      (http/stop service))
    (assoc component :service nil)))

(defn service
  [service-map]
  (map->ApiServer {:service-map service-map}))