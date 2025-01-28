(ns cheffy.components.auth
  (:require [com.stuartsierra.component :as component]
            [cognitect.aws.client.api :as aws]
            [clojure.data.json :as json])
  (:import (javax.crypto.spec SecretKeySpec)
           (javax.crypto Mac)
           (java.util Base64)
           (com.auth0.jwk UrlJwkProvider GuavaCachedJwkProvider)
           (com.auth0.jwt.interfaces RSAKeyProvider)
           (com.auth0.jwt.algorithms Algorithm)
           (com.auth0.jwt JWT)))

(defn validate-signature
  [{:keys [key-provider]} token]
  (let [algorithm (Algorithm/RSA256 key-provider)
        verifier (.build (JWT/require algorithm))
        verified-token (.verify verifier token)]
    (.getPayload verified-token)))

(defn decode-to-str
  [s]
  (String. (.decode (Base64/getUrlDecoder) s)))

(defn decode-token
  [token]
  (-> token
      (decode-to-str)
      (json/read-str)))

(defn verify-payload
  [{:keys [config]} {:strs [client_id iss token_use] :as payload}]
  (when-not
   (and
    (= (:client-id config) client_id)
    (= (:jwks config) iss)
    (contains? #{"access" "id"} token_use))
    (throw (ex-info "Token verification failed" {})))
  payload)

(defn verify-and-get-payload
  [auth token]
  (->> token
       (validate-signature auth)
       (decode-token)
       (verify-payload auth)))

(defn calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (let [hmac-sha256-algorithm "HmacSHA256"
        signing-key (SecretKeySpec. (.getBytes client-secret) hmac-sha256-algorithm)
        mac (doto (Mac/getInstance hmac-sha256-algorithm)
              (.init signing-key)
              (.update (.getBytes username)))
        raw-hmac (.doFinal mac (.getBytes client-id))]
    (.encodeToString (Base64/getEncoder) raw-hmac)))

(defn when-anomaly-throw
  [result]
  (when (contains? result :cognitect.anomalies/category)
    (throw (ex-info (:__type result) result))))

(defn create-cognito-account
  [{:keys [config cognito-idp]} {:keys [email password]}]
  (let [client-id (:client-id config)
        client-secret (:client-secret config)
        result (aws/invoke cognito-idp
                           {:op :SignUp
                            :request
                            {:ClientId client-id
                             :Username email
                             :Password password
                             :SecretHash (calculate-secret-hash
                                          {:client-id client-id
                                           :client-secret client-secret
                                           :username email})}})]
    (when-anomaly-throw result)
    [{:account/account-id (:UserSub result)
      :account/display-name email}]))

(defn confirm-cognito-account
  [{:keys [config cognito-idp]} {:keys [email confirmation-code]}]
  (let [client-id (:client-id config)
        client-secret (:client-secret config)
        result (aws/invoke cognito-idp
                           {:op :ConfirmSignUp
                            :request
                            {:ClientId client-id
                             :Username email
                             :ConfirmationCode confirmation-code
                             :SecretHash (calculate-secret-hash
                                          {:client-id client-id
                                           :client-secret client-secret
                                           :username email})}})]
    (when-anomaly-throw result)))

(defn cognito-log-in
  [{:keys [config cognito-idp]} {:keys [email password]}]
  (let [client-id (:client-id config)
        client-secret (:client-secret config)
        user-pool-id (:user-pool-id config)
        result (aws/invoke cognito-idp
                           {:op :AdminInitiateAuth
                            :request
                            {:ClientId client-id
                             :UserPoolId user-pool-id
                             :AuthFlow "ADMIN_USER_PASSWORD_AUTH"
                             :AuthParameters {"USERNAME" email
                                              "PASSWORD" password
                                              "SECRET_HASH" (calculate-secret-hash
                                                             {:client-id client-id
                                                              :client-secret client-secret
                                                              :username email})}}})]
    (when-anomaly-throw result)
    (:AuthenticationResult result)))


(defn cognito-refresh-token
  [{:keys [config cognito-idp]} {:keys [refresh-token sub]}]
  (let [client-id (:client-id config)
        client-secret (:client-secret config)
        user-pool-id (:user-pool-id config)
        result (aws/invoke cognito-idp
                           {:op :AdminInitiateAuth
                            :request
                            {:ClientId client-id
                             :UserPoolId user-pool-id
                             :AuthFlow "REFRESH_TOKEN_AUTH"
                             :AuthParameters {"REFRESH_TOKEN" refresh-token
                                              "SECRET_HASH" (calculate-secret-hash
                                                             {:client-id client-id
                                                              :client-secret client-secret
                                                              :username sub})}}})]
    (when-anomaly-throw result)
    (:AuthenticationResult result)))


(defn cognito-update-role
  [{:keys [cognito-idp config]} claims]
  (let [client-id (:client-id config)
        client-secret (:client-secret config)
        user-pool-id (:user-pool-id config)
        {:strs [sub]} claims
        result (aws/invoke cognito-idp
                           {:op :AdminAddUserToGroup
                            :request
                            {:ClientId client-id
                             :UserPoolId user-pool-id
                             :Username sub
                             :GroupName "cheffs"
                             :SecretHash (calculate-secret-hash
                                          {:client-id client-id
                                           :client-secret client-secret
                                           :username sub})}})]
    (when-anomaly-throw result)
    result))

(defn cognito-delete-user
  [{:keys [cognito-idp config]} claims]
  (let [user-pool-id (:user-pool-id config)
        {:strs [sub]} claims
        result (aws/invoke cognito-idp
                           {:op :AdminDeleteUser
                            :request
                            {:UserPoolId user-pool-id
                             :Username sub}})]
    (when-anomaly-throw result)))

(defrecord Auth [config cognito-idp key-provider]

  component/Lifecycle

  (start [component]
    (println ";; Starting Auth")
    (let [key-provider (-> (:jwks config)
                           (UrlJwkProvider.)
                           (GuavaCachedJwkProvider.))]
      (assoc component
             :cognito-idp (aws/client {:api :cognito-idp})
             :key-provider (reify RSAKeyProvider
                             (getPublicKeyById [_ kid]
                               (.getPublicKey (.get key-provider kid)))

                             (getPrivateKey [_]
                               nil)

                             (getPrivateKeyId [_]
                               nil)))))

  (stop [component]
    (println ";; Stopping Auth")
    (assoc component
           :cognito-idp nil
           :key-provider nil)))

(defn service
  [config]
  (map->Auth {:config config}))