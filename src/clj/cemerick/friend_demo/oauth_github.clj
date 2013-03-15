(ns ^{:name "GitHub using OAuth2"
      :doc "Authenticating via GitHub using OAuth2 [EXPERIMENTAL]"}
  cemerick.friend-demo.oauth-github
  (:require [cemerick.friend-demo.misc :as misc]
            [cemerick.friend-demo.users :as users :refer (users)]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [friend-oauth2.workflow :as oauth2]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [compojure.core :as compojure :refer (GET defroutes)]
            (compojure [handler :as handler]
                       [route :as route])
            [ring.util.response :as resp]
            [ring.util.codec :as codec]
            [hiccup.page :as h]
            [hiccup.element :as e]))

(defn- call-github
  [endpoint access-token]
  (-> (format "https://api.github.com%s%s&access_token=%s"
        endpoint
        (when-not (.contains endpoint "?") "?")
        access-token)
    http/get
    :body
    (json/parse-string (fn [^String s] (keyword (.replace s \_ \-))))))

;; This sort of blind memoization is *bad*. Please don't do this in your real apps.
;; Go use an appropriate cache from https://github.com/clojure/core.cache
(def get-public-repos (memoize (partial call-github "/user/repos?type=public")))
(def get-github-handle (memoize (comp :login (partial call-github "/user"))))

(compojure/defroutes routes
  (GET "/" req
    (h/html5
      misc/pretty-head
      (misc/pretty-body
        (misc/github-link req)
        [:h2 "Authenticating via GitHub using OAuth2 [EXPERIMENTAL]"]
        [:h3 "Current Status " [:small "(this will change when you log in/out)"]]
        [:p (if-let [identity (friend/identity req)]
              [:span "Logged in as GitHub user " [:strong (get-github-handle (:current identity))]
                     " with GitHub OAuth2 access token " (:current identity)]
              "anonymous user")]
        
        [:h3 [:a {:href "github.callback"} "Login with GitHub"]]
        
        (when-let [{access-token :access_token} (friend/current-authentication req)]
          [:div
           [:h3 "Some of your public repositories on GitHub, obtained using the access token above:"]
           [:ul (for [repo (get-public-repos access-token)]
                  [:li (:full-name repo)])]])
        
        [:h3 "Authorization demos"]
        [:p "Each of these links require particular roles (or, any authentication) to access. "
            "If you're not authenticated, you will be redirected to a dedicated login page. "
            "If you're already authenticated, but do not meet the authorization requirements "
            "(e.g. you don't have the proper role), then you'll get an Unauthorized HTTP response."]
        [:ul [:li (e/link-to (misc/context-uri req "role-user") "Requires the `user` role")]
         ;[:li (e/link-to (misc/context-uri req "role-admin") "Requires the `admin` role")]
         [:li (e/link-to (misc/context-uri req "requires-authentication")
                "Requires any authentication, no specific role requirement")]]
        [:h3 "Logging out"]
        [:p (e/link-to (misc/context-uri req "logout") "Click here to log out") "."])))
  (GET "/logout" req
    (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/requires-authentication" req
    (friend/authenticated "Thanks for authenticating!"))
  (GET "/role-user" req
    (friend/authorize #{::users/user} "You're a user!"))
  #_(GET "/role-admin" req
    (friend/authorize #{::users/admin} "You're an admin!")))

(def client-config
  {:client-id (System/getenv "github_client_id")
   :client-secret (System/getenv "github_client_secret")
   ;; TODO get friend-oauth2 to support :context, :path-info
   :callback {:domain "http://friend-demo.herokuapp.com" :path "/oauth-github/github.callback"}})

(def uri-config
  {:authentication-uri {:url "https://github.com/login/oauth/authorize"
                        :query {:client_id (:client-id client-config)
                                :response_type "code"
                                :redirect_uri (oauth2/format-config-uri client-config)
                                :scope "user"}}

   :access-token-uri {:url "https://github.com/login/oauth/access_token"
                      :query {:client_id (:client-id client-config)
                              :client_secret (:client-secret client-config)
                              :grant_type "authorization_code"
                              :redirect_uri (oauth2/format-config-uri client-config)
                              :code ""}}})

(def page (handler/site
            (friend/authenticate
              routes
              {:allow-anon? true
               :default-landing-uri (str (misc/ns->context *ns*) "/")
               :unauthorized-handler #(-> (h/html5 [:h2 "You do not have sufficient privileges to access " (:uri %)])
                                        resp/response
                                        (resp/status 401))
               :workflows [(oauth2/workflow
                             {:client-config client-config
                              :uri-config uri-config
                              :config-auth {:roles #{::users/user}}
                              :access-token-parsefn #(-> % :body codec/form-decode (get "access_token"))})]})))