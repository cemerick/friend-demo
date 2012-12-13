(ns ^{:name "HTTP Basic"
      :doc "Using HTTP Basic to authenticate to a Ring app"}
  cemerick.friend-demo.http-basic
  (:require [cemerick.friend-demo.users :refer (users)]
            [cemerick.friend-demo.misc :refer (context-uri request-url github-url-for)]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            
            [compojure.core :refer (GET defroutes)]
            [compojure.handler :refer (site)]
            [hiccup.page :as h]
            [hiccup.element :as e]
            [clojure.string :as str]))

(defroutes app*
  (GET "/requires-authentication" req
       (friend/authenticated (str "You have successfully authenticated as "
                                  (friend/current-authentication)))))

(def secured-app (friend/authenticate
                   app*
                   ;; TODO make :allow-anon? true once this is fixed:
                   ;; https://github.com/cemerick/friend/issues/38
                   {:allow-anon? false
                    :workflows [(workflows/http-basic
                                  :credential-fn #(creds/bcrypt-credential-fn @users %)
                                  :realm "Friend demo")]}))

(def app (site secured-app))

(defroutes page
  (GET "/" req
      (h/html5
         [:div {:style "float:right; width:50%"}
          [:a {:href (github-url-for (-> req :demo :ns-name))} "View source"]]
         [:h2 (-> req :demo :name)]
         [:p
          "Attempting to access " (e/link-to {:id "interactive_url"}
                                             (context-uri req "requires-authentication")
                                             "this link")
          " will issue a challenge for your user-agent (browser) to provide HTTP Basic credentials. "
          "Once authenticated, all the authorization options available in Friend are available to restrict the permissions of particular users."]
         [:p "Please note that Chrome (and maybe other browsers) silently save HTTP Basic credentials for the duration of the session (and resend them automatically!), so "
               (e/link-to (context-uri req "/logout") "logging out")
               " won't work as expected."]
         [:p
          "You can access resources requiring HTTP Basic authentication trivially in any HTTP client (like `curl`) with a URL such as:"]
         [:code "curl "
          (str/replace (str (request-url req) "/requires-authentication")
                       #"://" #(str % (-> @users first val :username (str ":clojure@"))))]
         [:p "You can combine this with Friend's \"channel security\" middleware to enforce the "
          "use of SSL, making this a good recipe for controlling access to web service APIs."
          " Head over to " (e/link-to (context-uri req "/https-basic") "HTTPS Basic")
          " for a demo."])))