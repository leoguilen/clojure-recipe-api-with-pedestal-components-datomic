(ns cheffy.recipe-tests 
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.stuartsierra.component.repl :as cr]
   [io.pedestal.http :as http]
   [io.pedestal.test :as pt]
   [cheffy.test-system :as ts]))

(def ^:private service-fn
  (-> cr/system :api-server :service ::http/service-fn))

(def ^:private recipe-id (atom nil))

(def ^:private step-id (atom nil))

(def ^:private ingredient-id (atom nil))

(deftest recipe-tests
  (testing "List recipes"
    (testing "with auth --public and drafts"
      (let [{:keys [status body]} (-> (pt/response-for
                                       service-fn
                                       :get "/recipes"
                                       :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})
                                      (update :body ts/transit-read))]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (vector? (:drafts body)))))
    (testing "without auth --public"
      (let [{:keys [status body]} (-> (pt/response-for
                                       service-fn
                                       :get "/recipes")
                                      (update :body ts/transit-read))]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (nil? (:drafts body))))))

  (testing "Create recipe"
    (let [{:keys [status body]} (-> (pt/response-for
                                     service-fn
                                     :post "/recipes"
                                     :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                               "Content-Type" "application/transit+json"}
                                     :body (ts/transit-write {:name "name"
                                                              :public true
                                                              :prep-time 30
                                                              :img "img"}))
                                    (update :body ts/transit-read))]
      (reset! recipe-id (:recipe-id body))
      (is (= 201 status))))

  (testing "Retrieve recipe"
    (let [{:keys [status]} (-> (pt/response-for
                                service-fn
                                :get (str "/recipes/" @recipe-id)
                                :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"}))]
      (is (= 200 status))))

  (testing "Update recipe"
    (let [{:keys [status]} (pt/response-for
                            service-fn
                            :put (str "/recipes/" @recipe-id)
                            :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                      "Content-Type" "application/transit+json"}
                            :body (ts/transit-write {:name "updated name"
                                                     :public true
                                                     :prep-time 30
                                                     :img "img"}))]
      (is (= 204 status))))

  (testing "Create recipe step"
    (let [{:keys [status body]} (-> (pt/response-for
                                     service-fn
                                     :post "/steps"
                                     :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                               "Content-Type" "application/transit+json"}
                                     :body (ts/transit-write {:recipe-id @recipe-id
                                                              :description "new step"
                                                              :sort-order 1}))
                                    (update :body ts/transit-read))]
      (reset! step-id (:step-id body))
      (is (= 201 status))))

  (testing "Update recipe step"
    (let [{:keys [status]} (pt/response-for
                            service-fn
                            :put (str "/steps/" @step-id)
                            :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                      "Content-Type" "application/transit+json"}
                            :body (ts/transit-write {:recipe-id @recipe-id
                                                     :step-id @step-id
                                                     :description "update step"
                                                     :sort-order 1}))]
      (is (= 204 status))))

  (testing "Delete recipe step"
    (let [{:keys [status]} (pt/response-for
                            service-fn
                            :delete (str "/steps/" @step-id)
                            :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                      "Content-Type" "application/transit+json"})]
      (is (= 204 status))))

  (testing "create-ingredient"
    (let [{:keys [status body]} (-> (pt/response-for
                                     service-fn
                                     :post "/ingredients"
                                     :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                               "Content-Type" "application/transit+json"}
                                     :body (ts/transit-write {:recipe-id @recipe-id
                                                              :name "ingredient name"
                                                              :amount 500
                                                              :measure "something"
                                                              :sort-order 1}))
                                    (update :body ts/transit-read))]
      (reset! ingredient-id (:ingredient-id body))
      (is (= 201 status))))

  (testing "update-ingredient"
    (let [{:keys [status body]} (pt/response-for
                                 service-fn
                                 :put (str "/ingredients/" @ingredient-id)
                                 :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                           "Content-Type" "application/transit+json"}
                                 :body (ts/transit-write {:recipe-id @recipe-id
                                                          :name "updated name"
                                                          :amount 500
                                                          :measure "something"
                                                          :sort-order 1}))]
      (is (= 204 status))))

  (testing "delete-ingredient"
    (let [{:keys [status body]} (pt/response-for
                                 service-fn
                                 :delete (str "/ingredients/" @ingredient-id)
                                 :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                           "Content-Type" "application/transit+json"})]
      (is (= 204 status))))

  (testing "Delete recipe"
    (let [{:keys [status]} (pt/response-for
                            service-fn
                            :delete (str "/recipes/" @recipe-id)
                            :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})]
      (is (= 204 status)))))