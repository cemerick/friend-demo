(ns ^{:name "Logging out"
      :doc "foo"}
  cemerick.friend-demo.logout
  (:require [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [compojure.core :refer (GET defroutes)]
            [compojure.handler :as handler]
            [hiccup.page :refer (html5)]
            [hiccup.element :as e]))

(defroutes routes
  (GET "/" req
       (html5 (e/link-to "logout" "Logging out")
              " will clear all retained Friend identities from your session."))
  (GET "/logout" req
       (html5 "You've been logged out; want to "
              [:a {:href "/"} "start over?"])))

(defroutes app
  (handler/api
    (friend/authenticate routes {:allow-anon? true})))