(ns bloknote.server
  (:require [bloknote.data :as data]
            compojure.core
            compojure.route
            compojure.handler
            hiccup.core
            hiccup.page
            hiccup.element))

(defn layout [title & body]
  (hiccup.page/html5
    [:head
      [:meta {:http-equiv "Content-Type" :content "text/html;charset=utf-8"}]
      [:link {:rel "shortcut icon" :href "/i/favicon.ico" :type "image/x-icon"}]
      (hiccup.page/include-css "/i/style.css")
      (hiccup.element/javascript-tag "var CLOSURE_NO_DEPS = true;")
      (hiccup.page/include-js "/i/xregexp-min.js")
      (hiccup.page/include-js "/i/xregexp-unicode-base.js")
      (hiccup.page/include-js "/i/jquery-1.7.2.min.js")
      [:title title]]
    [:body body]))

(defn index-page []
  (layout "Bloknote"
    [:a {:href "tonsky/"} "Log in as " [:strong "tonsky"]]))

(defn user-page [user]
  (layout (str "Bloknotes of " user)
    [:ul#titles]
    [:textarea {:id "ta"}]
    [:div#sheet.focused
      [:u#aux.aux "Loading..."]
      [:div#cur-begin]
      [:div#cur-end]]
    (hiccup.page/include-js "/i/bloknote.js")
    (hiccup.element/javascript-tag (str "bloknote.sheet.init_user('" user "');"))))

(defn api-list [user]
  {:body    (pr-str (data/list-db user))
   :headers {"Content-Type" "text/plain; charset=utf-8"}})

(defn api-load [user id]
  (Thread/sleep 1000)
  {:body    (pr-str (data/load-db user id))
   :headers {"Content-Type" "text/plain; charset=utf-8"}})

(defn api-save [user id bloknote]
  (data/save-db user id (read-string bloknote))
  {:status 201
   :body (str "Bloknote " id " saved")
   :headers {"Content-Type" "text/plain; charset=utf-8"}})

(compojure.core/defroutes main-routes
  (compojure.route/resources "/i")
  (compojure.core/GET  "/api/:user/"     [user]            (api-list user))
  (compojure.core/GET  "/api/:user/:id/" [user id]         (api-load user id))
  (compojure.core/POST "/api/:user/:id/" [user id payload] (api-save user id payload))
  (compojure.core/GET  "/:user/"         [user]            (user-page user))
  (compojure.core/GET  "/"               []                (index-page))
  (compojure.route/not-found "Page not found"))

(def app
  (compojure.handler/site main-routes))
