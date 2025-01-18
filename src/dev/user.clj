(ns user
  (:require
   [cheffy.routes :as routes]
   [clojure.edn :as edn]
   [io.pedestal.http :as http]))

(defonce system-ref (atom nil))

(defn start-dev
  []
  (let [config (-> "src/config/cheffy/development.edn" slurp edn/read-string)]
    (reset! system-ref
            (-> config
                (assoc ::http/routes routes/routes)
                (http/create-server)
                (http/start))))
  :started)

(defn stop-dev
  []
  (http/stop @system-ref)
  :stopped)

(defn restart-dev
  []
  (stop-dev)
  (start-dev)
  :restarted)

(comment
  (start-dev)
  (stop-dev)
  (restart-dev))