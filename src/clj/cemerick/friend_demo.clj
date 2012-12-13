(ns cemerick.friend-demo
  (:require (compojure handler [route :as route])
            [compojure.core :as compojure :refer (GET defroutes)]
            [hiccup.core :as h]
            [hiccup.element :as e]
            ring.adapter.jetty
            [bultitude.core :as b]))

(def ns-prefix "cemerick.friend-demo")

(defn- demo-vars
  [ns]
  {:namespace ns
   :ns-name (ns-name ns)
   :name (-> ns meta :name)
   :doc (-> ns meta :doc)
   :route-prefix (str "/" (-> ns ns-name name (subs (inc (count ns-prefix)))))
   :app (ns-resolve ns 'app)
   :page (ns-resolve ns 'page)})

(def the-menagerie (->> (b/namespaces-on-classpath :prefix ns-prefix)
                     distinct
                     (map #(do (require %) (the-ns %)))
                     (map demo-vars)
                     (filter :app)
                     (filter :page)))

(defroutes landing
  (GET "/" req (h/html [:html
                        [:body
                         [:h1 "Among Friends"]
                         [:small " (a collection of demonstration apps using "
                          (e/link-to "http://github.com/cemerick/friend" "Friend")
                          ", an authentication and authorization library for securing Clojure web services and applications.)"]
                         [:p "Each demo application is self-contained, interactive, and annotated with links to its source."]
                         [:h2 "Demonstrations"]
                         [:ol
                          (for [{:keys [name doc route-prefix]} the-menagerie]
                            [:li (e/link-to route-prefix [:strong name])
                             " — " doc])]]])))

(def site (apply compojure/routes
            landing
            (for [{:keys [app page route-prefix]} the-menagerie]
              (compojure/context route-prefix [] (compojure/routes page app)))))

(defn run
  []
  (defonce ^:private server
    (ring.adapter.jetty/run-jetty #'site {:port 8080 :join? false}))
  server)

(defn -main
  "For heroku."
  [port]
  (ring.adapter.jetty/run-jetty #'site {:port (Integer. port)}))

(in-ns 'user)