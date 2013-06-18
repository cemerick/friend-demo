(ns ^{:name "Multi-factor auth"
      :doc "Multi-factor Ring app authentication"}
  cemerick.friend-demo.multi-factor
  (:require [cemerick.friend-demo.users :refer (users)]
            [cemerick.friend-demo.misc :as misc :refer (context-uri request-url github-link)]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :refer (make-auth)]
            [cemerick.friend.credentials :as creds]
            
            [compojure.core :refer (GET POST routes defroutes)]
            [compojure.handler :refer (site)]
            [ring.util.response :as resp]
            [hiccup.page :as h]
            [hiccup.element :as e]))

(defn- login-page
  []
  (h/html5
    misc/pretty-head
    (misc/pretty-body
     [:form {:action "start" :method "POST" :class "columns small-4"}
      "Username: "
      [:input {:type "text" :name "username"}]
      [:input {:type "submit" :class "button" :value "Login"}]])))

(defn- pin-page
  [identity invalid-login?]
  (h/html5
    misc/pretty-head
    (misc/pretty-body
     (when invalid-login?
       [:p {:style "color:red"} "Sorry, that's not correct!"])
     [:p "Hello, " (:username identity) "; it looks like you're a "
      (-> identity :roles first name) "."]
     [:p "We've sent you a PIN (not really; it's always `1234` in this demo, but you could use something like Twilio to send a PIN); "
      "please enter it and your password here:"]
     [:form {:action "finish" :method "POST" :class "columns small-4"}
      [:div "Password: " [:input {:type "password" :name "password"}]]
      [:div "PIN: " [:input {:type "text" :name "pin"}]]
      [:input {:type "submit" :class "button" :value "Verify PIN"}]])))

(defn multi-factor
  [& {:keys [credential-fn] :as form-config}]
  (routes
    (GET "/login" req (login-page))
    (GET "/logout" req
      (friend/logout* (resp/redirect (str (:context req) "/"))))
    (POST "/start" {:keys [params session] :as request}
      (if-let [user-record (-> params :username credential-fn)]
        (-> (pin-page user-record false)
          resp/response
          (assoc :session session)
          (update-in [:session] assoc :user-record user-record))
        (resp/redirect (context-uri request "login"))))
    (POST "/finish" {{:keys [password pin]} :params
                     {:keys [user-record]} :session
                     :as request}
       (if (and user-record password
             (creds/bcrypt-verify password (:password user-record))
             (= pin (:pin user-record)))
         (make-auth (dissoc user-record :password)
           {::friend/workflow :multi-factor
            ::friend/redirect-on-auth? true})
         (pin-page user-record true)))))

(defroutes app*
  (GET "/requires-authentication" req
       (friend/authenticated (str "You have successfully authenticated as "
                                  (friend/current-authentication)))))

(def secured-app (friend/authenticate
                   app*
                   {:allow-anon? true
                    :login-uri "/login"
                    :workflows [(multi-factor :credential-fn @users)]}))

(def app (site secured-app))

(defroutes page
  (GET "/" req
    (h/html5
      misc/pretty-head
      (misc/pretty-body
       (github-link req)
       [:h2 (-> req :demo :name)]
       [:p "Clicking " (e/link-to (context-uri req "requires-authentication")
                         "this link")
           " will start a multi-factor authentication process, simluating one "
           "where a random PIN is sent to you via SMS.  (The PIN for this demo "
           "is always `1234`)."]
       [:h3 "Logging out"]
       [:p (e/link-to (misc/context-uri req "logout") "Click here to log out") "."]))))

