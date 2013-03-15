(ns cemerick.friend-demo
  (:require [cemerick.friend-demo.misc :as misc]
            (compojure handler [route :as route])
            [compojure.core :as compojure :refer (GET defroutes)]
            [hiccup.core :as h]
            [hiccup.element :as e]
            [ring.middleware.resource :refer (wrap-resource)]
            ring.adapter.jetty
            [bultitude.core :as b]))

(defn- demo-vars
  [ns]
  {:namespace ns
   :ns-name (ns-name ns)
   :name (-> ns meta :name)
   :doc (-> ns meta :doc)
   :route-prefix (misc/ns->context ns)
   :app (ns-resolve ns 'app)
   :page (ns-resolve ns 'page)})

(def the-menagerie (->> (b/namespaces-on-classpath :prefix misc/ns-prefix)
                     distinct
                     (map #(do (require %) (the-ns %)))
                     (map demo-vars)
                     (filter #(or (:app %) (:page %)))))

(defroutes landing
  (GET "/" req (h/html [:html
                        misc/pretty-head
                        (misc/pretty-body 
                         [:h1 {:style "margin-bottom:0px"} "Among Friends"]
                         [:p {:style "margin-top:0px"} [:small " (a collection of demonstration apps using "
                          (e/link-to "http://github.com/cemerick/friend" "Friend")
                          ", an authentication and authorization library for securing Clojure web services and applications.)"]]
                         [:p "Each demo application is self-contained, interactive, and annotated with links to its source."]
                         [:h2 "Demonstrations"]
                         [:ol
                          (for [{:keys [name doc route-prefix]} the-menagerie]
                            [:li (e/link-to (str route-prefix "/") [:strong name])
                             " — " doc])])])))

(defn- wrap-app-metadata
  [h app-metadata]
  (fn [req] (h (assoc req :demo app-metadata))))

(def site (apply compojure/routes
            landing
            (route/resources "/" {:root "META-INF/resources/webjars/foundation/4.0.4/"})
            (for [{:keys [app page route-prefix] :as metadata} the-menagerie]
              (compojure/context route-prefix []
                (wrap-app-metadata (compojure/routes (or page (fn [_])) (or app (fn [_]))) metadata)))))

(defn run
  []
  (defonce ^:private server
    (ring.adapter.jetty/run-jetty #'site {:port 8080 :join? false}))
  server)

(defn -main
  "For heroku."
  [port]
  (ring.adapter.jetty/run-jetty #'site {:port (Integer. port)}))