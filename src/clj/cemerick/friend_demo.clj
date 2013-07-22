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
                     (filter #(or (:app %) (:page %)))
                     (sort-by :ns-name)))

(defroutes landing
  (GET "/" req (h/html [:html
                        misc/pretty-head
                        (misc/pretty-body 
                         [:h1 {:style "margin-bottom:0px"}
                          [:a {:href "http://github.com/cemerick/friend-demo"} "Among Friends"]]
                         [:p {:style "margin-top:0px"} "…a collection of demonstration apps using "
                          (e/link-to "http://github.com/cemerick/friend" "Friend")
                          ", an authentication and authorization library for securing Clojure web services and applications."]
                         [:p "Implementing authentication and authorization for your web apps is generally a
necessary but not particularly pleasant task, even if you are using Clojure.
Friend makes it relatively easy and relatively painless, but I thought the
examples that the project's documentation demanded deserved a better forum than
to bit-rot in a markdown file or somesuch. So, what better than a bunch of live
demos of each authentication workflow that Friend supports (or is available via
another library that builds on top of Friend), with smatterings of
authorization examples here and there, all with links to the
generally-less-than-10-lines of code that makes it happen?  

Check out the demos, find the one(s) that apply to your situation, and
click the button on the right to go straight to the source for that demo:"]
                         [:div {:class "columns small-8"}
                          [:h2 "Demonstrations"]
                          [:ol
                           (for [{:keys [name doc route-prefix]} the-menagerie]
                             [:li (e/link-to (str route-prefix "/") [:strong name])
                              " — " doc])]]
                         [:div {:class "columns small-4"}
                          [:h2 "Credentials"]
                          [:p "All demo applications here that directly require user-provided credentials
recognize two different username/password combinations:"]
                          [:ul [:li [:code "friend/clojure"] " — associated with a \"user\" role"]
                               [:li [:code "friend-admin/clojure"] " — associated with an \"admin\" role"]]])])))

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
  [& [port]]
  (if port
    (ring.adapter.jetty/run-jetty #'site {:port (Integer. port)})
    (println "No port specified, exiting.")))
