(ns ^{:name "HTTP Basic over SSL (\"channel security\" middleware)"
      :doc "Same as 'HTTP Basic', but with the added condition that HTTPS/SSL is used (suitable for web service APIs)."}
  cemerick.friend-demo.https-basic
  (:require [cemerick.friend :as friend]
            [cemerick.friend-demo.http-basic :as basic]
            [compojure.handler :refer (site)]))


(def app (-> basic/secured-app
           ;; Only if you're behind a proxy that talks to your app over regular HTTP
           ;; (e.g. Heroku, Elastic Beanstalk, various proxy server configs)
           ;; do you need `requires-scheme-with-proxy`. Otherwise, use `requires-scheme`.
           (friend/requires-scheme-with-proxy :https)
           site))

(def page basic/page)