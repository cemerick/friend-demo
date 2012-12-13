(defproject com.cemerick/friend-demo "0.0.1-SNAPSHOT"
  :description "(eventually,) An über-demo of all that Friend has to offer."
  :url "http://github.com/cemerick/friend-demo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :resource-paths ["resources"]
  :source-paths ["src/clj"]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 
                 [com.cemerick/friend "0.1.2"]

                 [hiccup "1.0.1"]
                 [compojure "1.1.0"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 
                 [bultitude "0.1.7"]]
  
  ;; the final clean keeps AOT garbage out of the REPL's way, and keeps
  ;; the namespace metadata available at runtime
  :aliases  {"sanity-check" ["do" "clean," "compile" ":all," "clean"]}
  
  :ring {:handler cemerick.friend-demo/site
         :init cemerick.friend-demo/init})
