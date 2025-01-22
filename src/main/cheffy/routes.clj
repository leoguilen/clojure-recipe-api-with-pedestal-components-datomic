(ns cheffy.routes 
  (:require
   [cheffy.recipes :as recipes]
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]))

;; Table route spec
(defn routes
  []
  (route/expand-routes
   #{{:app-name :cheffy ::http/scheme :http ::http/host "localhost"}
     ["/recipes" :get recipes/list-recipes :route-name :list-recipes]
     ["/recipes" :post recipes/create-recipe :route-name :create-recipe]
     ["/recipes/:recipe-id" :get recipes/retrieve-recipe :route-name :retrieve-recipe]
     ["/recipes/:recipe-id" :put recipes/update-recipe :route-name :update-recipe]
     ["/recipes/:recipe-id" :delete recipes/delete-recipe :route-name :delete-recipe]}))

(comment
  ;; Terse route spec
  (def routes
    (route/expand-routes
     [[:cheffy :http "localhost"
       ["/recipes" {:get `list-recipes
                    :post `upsert-recipes}
        ["/:recipe-id" {:put [:update-recipe `upsert-recipes]}]]]])))
