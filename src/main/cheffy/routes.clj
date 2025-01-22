(ns cheffy.routes 
  (:require
   [cheffy.recipes :as recipes]
   [cheffy.recipes.ingredients :as ingredients]
   [cheffy.recipes.steps :as steps]
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
     ["/recipes/:recipe-id" :delete recipes/delete-recipe :route-name :delete-recipe]
     ;; steps
     ["/steps" :post steps/upsert-step :route-name :create-step]
     ["/steps/:step-id" :put steps/upsert-step :route-name :update-step]
     ["/steps/:step-id" :delete steps/delete-step :route-name :delete-step]
     ;; ingredients
     ["/ingredients" :post ingredients/upsert-ingredient :route-name :create-ingredient]
     ["/ingredients/:ingredient-id" :put ingredients/upsert-ingredient :route-name :update-ingredient]
     ["/ingredients/:ingredient-id" :delete ingredients/delete-ingredient :route-name :delete-ingredient]}))

(comment
  ;; Terse route spec
  (def routes
    (route/expand-routes
     [[:cheffy :http "localhost"
       ["/recipes" {:get `list-recipes
                    :post `upsert-recipes}
        ["/:recipe-id" {:put [:update-recipe `upsert-recipes]}]]]])))
