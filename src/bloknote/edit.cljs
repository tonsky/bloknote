(ns bloknote.edit
  (:require [jayq.core :as jq]
            [clojure.string :as str]
            [bloknote.cursor :as cur]
            [bloknote.utils :as u])
  (:use [jayq.util :only [log]]))

(def ^:dynamic *wrapw* 60)      ; wrap width
(defrecord Line [tags chars-count])
(defrecord Tag  [text dom])
(def $ta (jq/$ :#ta))

(defn create-tag [text]
  (let [escaped (u/escape-html text)
        class (cond
                (re-matches #"^[#].*" text) "header"
                :else                       "text")
        dom (case class
              "a" (jq/$ (str "<a href='" text "'>" escaped "</a>"))
              (jq/$ (str "<span class='tag_" class "'>" escaped "</span>")))]
    (Tag. text dom)))

(defn clone-tag [tag text]
  (let [$dom (jq/clone (.-dom tag))]
    (jq/inner $dom (u/escape-html text))
    (Tag. text $dom)))

(defn split-to-tags [par]
  (let [tag (create-tag par)]
    (map #(clone-tag tag %) (u/split-after par "\\-—–−\\s?"))))

(defn- append-tag [[lines last-line] tag]
  (let [tag-count  (count (.-text tag))
        line-count (.-chars-count last-line)]
    (cond
      (< (+ line-count tag-count) *wrapw*) [lines (Line. (u/append (.-tags last-line) tag) (+ line-count tag-count))]
      (< tag-count *wrapw*)                [(u/append lines last-line) (Line. [tag] tag-count)]
      :else                                (let [parts      (u/str-partition-all *wrapw* (.-text tag))
                                                 full-lines (for [tag-part (butlast parts)]
                                                                 (Line. [(clone-tag tag tag-part)] (count tag-part)))
                                                 last-part  (last parts)
                                                 new-last   (Line. [(clone-tag tag last-part)] (count last-part))]
                                             [(concat lines (if (not (empty? (.-tags last-line))) [last-line] []) full-lines) new-last]))))

(defn split-to-lines [par]
  (let [[lines last-line] (reduce append-tag [[] (Line. [] 0)] (split-to-tags par))
        lines             (u/append lines last-line)
        $dom              (jq/$ "<p></p>")]
    (doseq [line lines]
      (doseq [tag (.-tags line)]
        (jq/append $dom (.-dom tag)))
      (jq/append $dom (jq/$ "<br/>")))
    {:lines (into [] lines) :lines-count (count lines) :dom $dom :text par}))

(defn split-to-pars [text]
  (mapv split-to-lines (str/split text #"\n")))

(defn pos->offset [par ln ch]
  (reduce + ch (mapv :chars-count (take ln (:lines par)))))

(defn offset->pos [pars par off]
  ; (cur/Pos. par 0 0))
  (loop [ln 0, togo off]
    ; (log "Par " par ", ln " ln ", togo " togo ", off " off)
    (if (<= togo (cur/last-char pars par ln))
      (cur/Pos. par ln togo)
      (recur (inc ln) (- togo (cur/last-char pars par ln))))))

(defn gettext [pars pos-from pos-to]
  (let [pars (->> pars
               (take (inc (.-par pos-to))) 
               (drop (.-par pos-from)))
        off-begin (pos->offset (first pars) (.-ln pos-from) (.-ch pos-from))
        off-end   (pos->offset (last pars)  (.-ln pos-to)   (.-ch pos-to))
        texts     (mapv :text pars)
        texts     (-> texts
                    (update-in [(dec (count texts))] subs 0 off-end)
                    (update-in [0] subs off-begin))]
    (apply str texts)))

(defn normalize [pars pos-from pos-to]
  (let [[pos-from pos-to] (if (pos? (cur/cmp-pos pos-from pos-to))
                            [pos-to pos-from]
                            [pos-from pos-to])]
    [(cur/Pos. (.-par pos-from) (.-ln pos-from) (min (.-ch pos-from) (cur/last-char pars (.-par pos-from) (.-ln pos-from))))
     (cur/Pos. (.-par pos-to)   (.-ln pos-to)   (min (.-ch pos-to)   (cur/last-char pars (.-par pos-to) (.-ln pos-to))))]))

(defn replace-text [pars pos-from pos-to text]
  (let [[pos-from pos-to] (normalize pars pos-from pos-to)
        head     (gettext pars (cur/Pos. (.-par pos-from) 0 0) pos-from)
        par      (.-par pos-to)
        ln       (cur/last-line pars par)
        ch       (cur/last-char pars par ln)
        tail     (gettext pars pos-to (cur/Pos. par ln ch))
        new-text (str head text tail)
        to-add   (split-to-pars new-text)
        to-del   (drop (.-par pos-from) (take (inc (.-par pos-to)) pars))
        $anchor  (:dom (first to-del))
        new-pars (into [] (concat (take (.-par pos-from) pars) to-add (drop (inc (.-par pos-to)) pars)))]
    (doseq [p to-add]
      (jq/before $anchor (:dom p)))
    (doseq [p to-del]
      (jq/remove (:dom p)))
    (let [offset   (pos->offset (first to-del) (.-ln pos-from) (.-ch pos-from))
          new-from (cur/Pos. (.-par pos-from) 0 0)
          new-pos  (cur/chars-right new-from new-pars (+ offset (count text)))]
      (reset! cur/cur-begin new-pos)
      (reset! cur/cur-end new-pos))
    new-pars))

(defn maybe-insert
  ([pars]
    (let [text (jq/val $ta)]
      (when (not= "" text)
        (swap! pars replace-text @cur/cur-begin @cur/cur-end text)
        (jq/val $ta "")
        :processed)))
  ([e pars]
    (case (.-which e)
      8  (if (cur/collapsed?) ; backspace
           (swap! pars replace-text (cur/char-left @cur/cur-begin @pars) @cur/cur-end "")
           (swap! pars replace-text @cur/cur-begin @cur/cur-end ""))
      46 (if (cur/collapsed?) ; delete
           (swap! pars replace-text (cur/char-right @cur/cur-begin @pars) @cur/cur-end "")
           (swap! pars replace-text @cur/cur-begin @cur/cur-end ""))
      nil)))

(defn init []
  (.focus $ta))

