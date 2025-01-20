(ns cheffy.routes 
  (:require
   [cheffy.recipes :as recipes]
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]))

;; Table route spec
(def routes
  (route/expand-routes
   #{{:app-name :cheffy ::http/scheme :http ::http/host "localhost"}
     ["/recipes" :get recipes/list-recipes :route-name :list-recipes]
     ["/recipes" :post recipes/upsert-recipes :route-name :create-recipes]
     ["/recipes/:recipe-id" :put recipes/upsert-recipes :route-name :update-recipes]}))

;; Terse route spec
;; (def routes
;;   (route/expand-routes
;;    [[:cheffy :http "learnpedestal.com"
;;      ["/recipes" {:get `list-recipes
;;                   :post `upsert-recipes}
;;       ["/:recipe-id" {:put [:update-recipe `upsert-recipes]}]]]]))
