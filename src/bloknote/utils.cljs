(ns bloknote.utils
  (:require [clojure.string :as str]))

(defn split-after [text chars]
  (let [regex (js/RegExp. (str "(.*?[" chars "]+)(.*)"))]
    (loop [s text, acc []]
      (if-let [[_ g1 g2] (re-find regex s)]
        (recur g2 (conj acc g1))
        (conj acc s)))))

(defn escape-html [html]
  (-> html
    (str/replace #"&" "&amp;")
    (str/replace #"<" "&lt;")))

(defn str-partition-all [n text]
  (loop [tail text, acc []]
    (if (<= (count tail) n)
      (conj acc tail)
      (recur (subs tail n) (conj acc (subs tail 0 n))))))

(defn append [coll & els]
  (concat coll els))
