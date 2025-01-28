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

(defn calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (let [hmac-sha256-algorithm "HmacSHA256"
        signing-key (javax.crypto.spec.SecretKeySpec. (.getBytes client-secret) hmac-sha256-algorithm)
        mac (doto (javax.crypto.Mac/getInstance hmac-sha256-algorithm)
              (.init signing-key)
              (.update (.getBytes username)))
        raw-hmac (.doFinal mac (.getBytes client-id))]
    (.encodeToString (java.util.Base64/getEncoder) raw-hmac)))

(comment

  (:auth cr/system)

  (calculate-secret-hash {:client-id "client_id"
                          :client-secret "client_secret"
                          :username "username"})

  (require '[cognitect.aws.client.api :as aws])

  (def cognito-idp (aws/client {:api :cognito-idp}))

  (aws/ops cognito-idp)

  (aws/doc cognito-idp :SignUp)

  (aws/invoke cognito-idp
              {:op :SignUp
               :request {:ClientId "1448u1digq51ulcj6kc4hmhf20"
                         :Username "cheffy@app.com"
                         :Password "Cheffy25#"
                         :SecretHash (calculate-secret-hash {:client-id "1448u1digq51ulcj6kc4hmhf20"
                                                             :client-secret "1csq8o6dgf046ljt0jhcqihvv8ua336sob4kl20ev0majec6ojjr"
                                                             :username "cheffy@app.com"})}})
    (aws/invoke cognito-idp
              {:op :AdminInitiateAuth
               :request {:ClientId "1448u1digq51ulcj6kc4hmhf20"
                         :UserPoolId "us-west-2_rR5LUUlMx"
                         :AuthFlow "ADMIN_USER_PASSWORD_AUTH"
                         :AuthParameters {"USERNAME" "cheffy@app.com"
                                          "PASSWORD" "Cheffy25#"
                                          "SECRET_HASH" (calculate-secret-hash {:client-id "1448u1digq51ulcj6kc4hmhf20"
                                                                                :client-secret "1csq8o6dgf046ljt0jhcqihvv8ua336sob4kl20ev0majec6ojjr"
                                                                                :username "cheffy@app.com"})}}})

  (-> cr/system :api-server :service :env)

  (-> (transit-write {:name "name"
                      :public true
                      :prep-time 30
                      :img "img"})
      (transit-read))

  (pt/response-for
   (-> cr/system :api-server :service ::http/service-fn)
   :delete "/recipes/833adf91-df0e-47ab-96ad-6c7637835ead"
   :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})

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
   :get "/recipes/833adf91-df0e-47ab-96ad-6c7637835ead"
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