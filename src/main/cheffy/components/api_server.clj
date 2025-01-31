(ns cheffy.components.api-server
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [cheffy.routes :as routes]
            [io.pedestal.interceptor :as interceptor]))

(defn inject-system
  [system]
  (interceptor/interceptor
   {:name ::inject-system
    :enter (fn [ctx]
             (update-in ctx [:request] merge system))}))

(defn create-cheffy-server
  [service-map]
  (http/create-server (http/dev-interceptors service-map)))

(defn cheffy-interceptors
  [service-map sys-interceptors]
  (let [default-interceptors (-> service-map
                                 (http/default-interceptors)
                                 ::http/interceptors)
        interceptors (into [] (concat
                               (butlast default-interceptors)
                               sys-interceptors
                               [(last default-interceptors)]))]
    (assoc service-map ::http/interceptors interceptors)))

(defrecord ApiServer [service-map service database auth]

  component/Lifecycle

  (start [component]
    (println ";; Stating API Server")
    (let [service (-> service-map
                      (assoc ::http/routes #(routes/routes))
                      (cheffy-interceptors [(inject-system {:system/database database :system/auth auth})])
                      (create-cheffy-server)
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