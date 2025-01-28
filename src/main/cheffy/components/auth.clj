(ns cheffy.components.auth 
  (:require
   [clojure.data.json :as json]
   [cognitect.aws.client.api :as aws]
   [com.stuartsierra.component :as component]) 
  (:import
   [com.auth0.jwt JWT]
   [com.auth0.jwt.algorithms Algorithm]))

(defn validate-signature
  [{:keys [key-provider]} token]
  (let [algorithm (Algorithm/RSA256 key-provider)
        verifier (.build (JWT/require algorithm))
        verified-token (.verify verifier token)]
    (.getPayload verified-token)))

(defn decode-to-str
  [s]
  (String. (.decode (java.util.Base64/getUrlDecoder) s)))

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

(defn ^:private calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (let [hmac-sha256-algorithm "HmacSHA256"
        signing-key (javax.crypto.spec.SecretKeySpec. (.getBytes client-secret) hmac-sha256-algorithm)
        mac (doto (javax.crypto.Mac/getInstance hmac-sha256-algorithm)
              (.init signing-key)
              (.update (.getBytes username)))
        raw-hmac (.doFinal mac (.getBytes client-id))]
    (.encodeToString (java.util.Base64/getEncoder) raw-hmac)))

(defn ^:private when-anomaly-throw
  [result]
  (when (contains? result :cognitect.anomalies/category)
    (throw (ex-info (:__type result) result))))

(defn create-cognito-account
  [{:keys [config cognito-idp]} {:keys [email password]}]
  (let [client-id (:client-id config)
        client-secret (:client-secret config)
        result (aws/invoke cognito-idp
                           {:op :SignUp
                            :request {:ClientId client-id
                                      :Username email
                                      :Password password
                                      :SecretHash (calculate-secret-hash {:client-id client-id
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
                            :request {:ClientId client-id
                                      :Username email
                                      :ConfirmationCode confirmation-code
                                      :SecretHash (calculate-secret-hash {:client-id client-id
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
                            :request {:ClientId client-id
                                      :UserPoolId user-pool-id
                                      :AuthFlow "ADMIN_USER_PASSWORD_AUTH"
                                      :AuthParameters {"USERNAME" email
                                                       "PASSWORD" password
                                                       "SECRET_HASH" (calculate-secret-hash {:client-id client-id
                                                                                             :client-secret client-secret
                                                                                             :username email})}}})]
    (when-anomaly-throw result)
    (:AuthenticationResult result)))

(defrecord Auth [config cognito-idp key-provider]
  component/Lifecycle

  (start [component]
    (println ";; Starting Auth")
    (let [key-provider (-> (:jwks config)
                           (com.auth0.jwk.UrlJwkProvider.)
                           (com.auth0.jwk.GuavaCachedJwkProvider.))]
      (assoc component
             :cognito-idp (aws/client {:api :cognito-idp})
             :key-provider (reify com.auth0.jwt.interfaces.RSAKeyProvider
                             (getPublicKeyById [_ kid]
                               (.getPublicKey (.get key-provider kid)))
                             (getPrivateKey [_] nil)
                             (getPrivateKeyId [_] nil)))))

  (stop [component]
    (println ";; Stopping Auth")
    (assoc component
           :cognito-idp nil
           :key-provider nil)))

(defn service
  [config]
  (map->Auth {:config config}))
