(ns cheffy.conversations-tests
  (:require [clojure.test :refer :all]
            [io.pedestal.test :as pt]
            [io.pedestal.http :as http]
            [cheffy.test-system :as ts]
            [com.stuartsierra.component.repl :as cr]))


(def service-fn (-> cr/system :api-server :service ::http/service-fn))

(def conversation-id (atom nil))

(deftest conversation-tests

  (testing "create-messages"
    (testing "without-conversation-id"
      (let [{:keys [status body]} (-> (pt/response-for
                                       service-fn
                                       :post "/conversations"
                                       :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                                 "Content-Type" "application/transit+json"}
                                       :body (ts/transit-write {:to "mike@mailinator.com"
                                                                :message-body "new message"}))
                                      (update :body ts/transit-read))]
        (reset! conversation-id (:conversation-id body))
        (is (= 201 status))))

    (testing "with-conversation-id"
      (let [{:keys [status body]} (-> (pt/response-for
                                       service-fn
                                       :post (str "/conversations/" @conversation-id)
                                       :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                                 "Content-Type" "application/transit+json"}
                                       :body (ts/transit-write {:to "mike@mailinator.com"
                                                                :message-body "second message"}))
                                      (update :body ts/transit-read))]
        (is (= 201 status)))))

  (testing "list-conversations"
    (let [{:keys [status body]} (-> (pt/response-for
                                     service-fn
                                     :get "/conversations"
                                     :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})
                                    (update :body ts/transit-read))]
      (is (= 200 status))))

  (testing "list-messages"
    (let [{:keys [status body]} (-> (pt/response-for
                                     service-fn
                                     :get (str "/conversations/" @conversation-id)
                                     :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})
                                    (update :body ts/transit-read))]
      (is (= 200 status))))

  (testing "clear-notifications"
    (let [{:keys [status body]} (pt/response-for
                                 service-fn
                                 :delete (str "/conversations/" "362d06c7-2702-4273-bcc3-0c04d2753b6f")
                                 :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})]
      (is (= 204 status)))))