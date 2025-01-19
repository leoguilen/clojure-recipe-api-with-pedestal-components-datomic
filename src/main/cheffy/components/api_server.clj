(ns cheffy.components.api-server 
  (:require
   [cheffy.routes :as routes]
   [com.stuartsierra.component :as component]
   [io.pedestal.http :as http]
   [io.pedestal.interceptor :as interceptor]))

(defn inject-system
  [system]
  (interceptor/interceptor
   {:name ::inject-system
    :enter (fn [ctx] (update-in ctx [:request] merge system))}))

(defrecord ApiServer [service-map service database]
  
  component/Lifecycle
  
  (start [component]
    (println ";; Starting API server")
    (let [service (-> service-map
                      (assoc ::http/routes routes/routes)
                      (update-in [::http/interceptors] conj (inject-system {:system/database database}))
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