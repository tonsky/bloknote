(ns bloknote.editor
  (:require [jayq.core :as jq]
            [clojure.string :as str]
            [bloknote.utils :as u]
            [bloknote.cursor :as cur]
            [bloknote.edit :as edit]
            [cljs.reader :as reader])
  (:use [jayq.util :only [log clj->js js->clj]]))

(defrecord Par  [lines lines-count offset first-line-offset])
(defrecord Line [tags chars-count offset])
(defrecord Tag  [text name attrs])

;    # Header
;   ## Header 2
;      _em_, *em*
;      __strong__, **stong**
;      link http://link.com link
;      > blockquote
;        multi-level
;    * list
;    + list
;    - list
;   1. list
;   2. list
; 100. list

(def ^:dynamic par-width 64)
(def ^:dynamic left-margin 7)

(declare split-to-tags)
(declare par-style)
(declare take-line)

(defn text->par [text]
  (let [tags                       (split-to-tags text)
        {:keys [offset first-line-offset]} (par-style text)
        [first-line tags]          (take-line tags first-line-offset)
        lines                      (loop [lines '()
                                          tags   tags]
                                     (if tags
                                       (let [[line tags] (take-line tags offset)]
                                         (recur (conj lines line) tags))
                                       lines))]
    (conj lines first-line)))
  
(defn par-style [text]
  (condp re-matches text
    #"(#+ ).*"          :>> (fn [[_ m]] {:style :header, :offset 0, :first-line-offset (- (min (count m) left-margin))})
    #"(> ).*"                           {:style :blockquote, :offset 2, :first-line-offset 0}
    #"([*+\-−—–•]+ ).*" :>> (fn [[_ m]] {:style :ul, :offset 0, :first-line-offset (- (min (count m) left-margin))})
    #"((?:\d+\.)+ ).*"  :>> (fn [[_ m]] {:style :ol, :offset 0, :first-line-offset (- (min (count m) left-margin))})
                                        {:style :text, :offset 0, :first-line-offset 0}))

(defn normalize-href [s]
  (if (re-matches #"https?://.*" s)
    s
    (str "http://" s)))

(def transforms [
  [#"(.*?\s|^)(\*\*)([^\s].*?[^\s])(\*\*)(\s.*|$)" (fn [tag m] [(assoc tag  :text (nth m 1))
                                                                (assoc tag  :text (nth m 2))
                                                                (Tag. (nth m 3) :strong nil)
                                                                (assoc tag  :text (nth m 4))
                                                                (assoc tag  :text (nth m 5))])]
  [#"(.*?\s|^)(\*)([^\s].*?[^\s])(\*)(\s.*|$)"     (fn [tag m] [(assoc tag  :text (nth m 1))
                                                                (assoc tag  :text (nth m 2))
                                                                (Tag. (nth m 3) :em nil)
                                                                (assoc tag  :text (nth m 4))
                                                                (assoc tag  :text (nth m 5))])]
  [(js/RegExp. (str "(.*?)("
                      "(?:https?:\\/\\/|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}\\/)"
                      "(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+"
                      "(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\\\".,<>?«»“”‘’])"
                    ")(.*)") "i")                  (fn [tag m] [(assoc tag  :text (nth m 1))
                                                                (Tag. (nth m 2) :a {:href (normalize-href (nth m 2))})
                                                                (assoc tag  :text (last m)) ])]
])

(defn split-one-tag-by-re [tag [re action]]
  (if-let [groups (re-matches re (.-text tag))]
    (do
      ; (.log js/console (str "Match: " groups " for tag: <" (.-name tag) ">" (.-text tag) "</>"))
      (let [res (->> (action tag groups)
                     (remove (comp empty? :text)))]
        (if (= res [tag]) nil res))) ;; something should change
    nil))

(defn split-one-tag [tag]
  ; (.log js/console (str "Entering tag " (.-name tag) ": " (.-text tag)))
  (if-let [tags (some #(split-one-tag-by-re tag %) transforms)]
    (flatten (map split-one-tag tags))
    [tag]))

(defn split-to-tags [text]
  (split-one-tag (Tag. text :span nil)))
