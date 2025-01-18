(ns cheffy.routes 
  (:require
   [io.pedestal.http.route :as route]))

(defn- list-recipes
  [request]
  {:status 200
   :body "List recipes"})

(defn- upsert-recipes
  [request]
  {:status 200
   :body "Upsert recipes"})

;; Table route spec
(def routes
  (route/expand-routes
   #{{:app-name :cheffy :schema :http :host "learnpedestal.com"}
     ["/recipes" :get list-recipes :route-name :list-recipes]
     ["/recipes" :post upsert-recipes :route-name :create-recipes]
     ["/recipes/:recipe-id" :put upsert-recipes :route-name :update-recipes]}))

;; Terse route spec
;; (def routes
;;   (route/expand-routes
;;    [[:cheffy :http "learnpedestal.com"
;;      ["/recipes" {:get `list-recipes
;;                   :post `upsert-recipes}
;;       ["/:recipe-id" {:put [:update-recipe `upsert-recipes]}]]]]))
