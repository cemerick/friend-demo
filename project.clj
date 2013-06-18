(defproject com.cemerick/friend-demo "0.0.2-SNAPSHOT"
  :description "(eventually,) An über-demo of all that Friend has to offer."
  :url "http://github.com/cemerick/friend-demo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :resource-paths ["resources"]
  :source-paths ["src/clj"]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 
                 [com.cemerick/friend "0.2.0-SNAPSHOT"]

                 [compojure "1.1.4"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 
                 ;; only used for the oauth-related demos
                 [friend-oauth2 "0.0.3"]
                 
                 ;; only used to generate demo app pages
                 [hiccup "1.0.1"]
                 
                 ;; only used to discover demo app namespaces
                 [bultitude "0.1.7"]
                 
                 ;; only used for foundation js/css
                 [org.webjars/foundation "4.0.4"]]
  
  ;; the final clean keeps AOT garbage out of the REPL's way, and keeps
  ;; the namespace metadata available at runtime
  :aliases  {"sanity-check" ["do" "clean," "compile" ":all," "clean"]}
  
  :ring {:handler cemerick.friend-demo/site
         :init cemerick.friend-demo/init})
