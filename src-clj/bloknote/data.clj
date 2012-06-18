(ns bloknote.data
  (:use     clojure.java.io)
  (:require [clojure.string :as str]))

(defn to-file [user]
  (file (str "data/" user ".clj")))

(defmacro sync-for [user & body]
  `(locking (.intern ~user)
      ~@body)) 

(defn read-user [user]
  (sync-for user
    (-> (to-file user)
        slurp
        read-string)))

(defn default []
  (rand-nth [
    {:text "# War and Peace\n=============\n\nBy Leo Tolstoy" :cur-begin [3 0 0] :cur-end [3 0 0]}
    {:text "# Mother Night\n=============\n\nBy Kurt Vonneguth" :cur-begin [3 0 0] :cur-end [3 0 0]}
    {:text "# Hamlet\n=============\n\nBy William Shakespeare" :cur-begin [3 0 0] :cur-end [3 0 0]}
    {:text "# Great Expectations\n=============\n\nBy Charles Dickens" :cur-begin [3 0 0] :cur-end [3 0 0]}]))
           
(defn load-db [user id]
  (sync-for user
    (let [content (read-user user)]
      (get-in content [:bloknotes id] (default)))))

(defn save-db [user id bloknote]
  (sync-for user
    (let [f (to-file user)
          content (-> (read-user user)
                      (assoc-in [:bloknotes id] bloknote)
                      (assoc-in [:last-opened-id] id))]
      (spit f content))))

(defn title [b]
  (-> (:text b)
      (str/split #"\n" 2)
      first))

(defn list-db [user]
  (sync-for user
    (let [content (read-user user)
          titles  (for [[id b] (:bloknotes content)]
                       [(title b) id])
          last-opened-id (:last-opened-id content)]
      {:titles (sort titles)
       :last-opened-id last-opened-id 
       :last-opened (load-db user last-opened-id)})))