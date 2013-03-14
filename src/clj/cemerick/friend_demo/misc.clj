(ns cemerick.friend-demo.misc
  (:require [clojure.string :as str])
  (:import java.net.URI))

(def github-base-url
  "https://github.com/cemerick/friend-demo/blob/master/src/clj/")

(defn github-url-for
  [ns-name]
  (str github-base-url
       (-> (name ns-name)
         (.replace \. \/)
         (.replace \- \_))
       ".clj"))

(defn github-link
  [req]
  [:div {:style "float:right; width:10%"}
   [:a {:href (github-url-for (-> req :demo :ns-name))} "View source"]])

(defn resolve-uri
  [context uri]
  (let [context (if (instance? URI context) context (URI. context))]
    (.resolve context uri)))

(defn context-uri
  "Resolves a [uri] against the :context URI (if found) in the provided
   Ring request.  (Only useful in conjunction with compojure.core/context.)"
  [{:keys [context]} uri]
  (if-let [base (and context (str context "/"))]
    (str (resolve-uri base uri))
    uri))

(defn request-url
  "Returns the full URL that provoked the given Ring request as a string."
  [{:keys [scheme server-name server-port uri query-string]}]
  (let [port (when (or (and (= :http scheme) (not= server-port 80))
                       (and (= :https scheme) (not= server-port 443)))
               (str ":" server-port))]
    (str (name scheme) "://" server-name port uri
         (when query-string (str "?" query-string)))))

(def ns-prefix "cemerick.friend-demo")
(defn ns->context
  [ns]
  (str "/" (-> ns ns-name name (subs (inc (count ns-prefix))))))
