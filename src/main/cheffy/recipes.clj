(ns cheffy.recipes 
  (:require
    [cheffy.interceptors :as interceptors]))

(defn- list-recipes-response
  [request]
  {:status 200
   :body "List recipes"})

(def list-recipes
  [interceptors/db-interceptor list-recipes-response])

(defn- upsert-recipes-response
  [request]
  {:status 200
   :body "Upsert recipes"})

(def upsert-recipes
  [upsert-recipes-response])