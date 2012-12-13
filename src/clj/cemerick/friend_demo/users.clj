(ns cemerick.friend-demo.users
  (:require [cemerick.friend.credentials :refer (hash-bcrypt)]))

(def users (atom {"friend" {:username "friend"
                            :password (hash-bcrypt "clojure")
                            :roles #{::user}}
                  "friend-admin" {:username "friend-admin"
                                  :password (hash-bcrypt "clojure")
                                  :roles #{::admin}}}))

(derive ::admin ::user)