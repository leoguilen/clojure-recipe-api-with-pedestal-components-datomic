(ns user
  (:require [io.pedestal.http :as http]))

(def system-ref (atom nil))

(defn start-dev
  []
  (reset! system-ref
          (-> {::http/routes #{}
               ::http/type :jetty
               ::http/port 3000}
              (http/create-server)
              (http/start))))

(defn stop-dev
  []
  (http/stop @system-ref))

(println "Hello")

(comment
  (start-dev)
  (stop-dev))