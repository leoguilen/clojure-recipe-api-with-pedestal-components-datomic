(ns cheffy.components.auth 
  (:require
    [com.stuartsierra.component :as component]
    [cognitect.aws.client.api :as aws]))

(defn calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (let [hmac-sha256-algorithm "HmacSHA256"
        signing-key (javax.crypto.spec.SecretKeySpec. (.getBytes client-secret) hmac-sha256-algorithm)
        mac (doto (javax.crypto.Mac/getInstance hmac-sha256-algorithm)
              (.init signing-key)
              (.update (.getBytes username)))
        raw-hmac (.doFinal mac (.getBytes client-id))]
    (.encodeToString (java.util.Base64/getEncoder) raw-hmac)))

(defrecord Auth [config cognito-idp]
  component/Lifecycle
  
  (start [component]
    (println ";; Starting Auth")
    (assoc component :cognito-idp (aws/client {:api :cognito-idp})))
  
  (stop [component]
    (println ";; Stopping Auth")
    (assoc component :cognito-idp nil)))

(defn service
  [config]
  (map->Auth {:config config}))
