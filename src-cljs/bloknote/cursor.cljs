(ns bloknote.cursor
  (:require [jayq.core :as jq]
            [clojure.string :as str]
            [bloknote.utils :as u])
  (:use [jayq.util :only [log]]))

; [ {:lines [
;     {:tags [[:text "Some text"]
;             [:a "http:///"]]}
;     {:tags [[:text "more text"]]}]}]

(defrecord Pos [par ln ch])
(defn cmp [x y] 
  (let [c1 (compare (.-par x) (.-par y))
        c2 (compare (.-ln x) (.-ln y))
        c3 (compare (.-ch x) (.-ch y))]
    (if (zero? c1) (if (zero? c2) c3 c2) c1)))

(def ^:dynamic *chw* 10)        ; char width
(def ^:dynamic *lh* 30)         ; line height

(def cur-begin (atom (Pos. 0 0 0)))
(def cur-end   (atom (Pos. 0 0 0)))
(def $cur-begin (jq/$ :#cur-begin)) 
(def $cur-end   (jq/$ :#cur-end))

(defn last-char [pars par ln]
  (get-in pars [par :lines ln :chars-count]))

(defn last-line [pars par]
  (dec (get-in pars [par :lines-count])))

(defn last-par [pars]
  (dec (count pars)))

(defn line-up [pos pars]
  (let [{:keys [par ln ch]} pos]
    (cond
      (> ln 0)  (Pos. par (dec ln) ch )
      (> par 0) (Pos. (dec par) (last-line pars (dec par)) ch)
      :else     (Pos. 0 0 0))))

(defn line-down [pos pars]
  (let [{:keys [par ln ch]} pos]
    (cond
      (< ln (last-line pars par)) (Pos. par (inc ln) ch)
      (< par (last-par pars))     (Pos. (inc par) 0 ch)
      :else                       (Pos. par ln (last-char pars par ln)))))

(defn char-left [pos pars]
  (let [{:keys [par ln ch]} pos]
    (cond
      (> ch 0)  (Pos. par ln (dec (min ch (last-char pars par ln))))
      (> ln 0)  (Pos. par (dec ln) (last-char pars par (dec ln)))
      (> par 0) (let [par (dec par)
                      ln  (last-line pars par)]
                  (Pos. par ln (last-char pars par ln)))
      :else     pos)))

(defn char-right [pos pars]
  (let [{:keys [par ln ch]} pos]
    (cond
      (< ch (last-char pars par ln)) (Pos. par ln (inc ch))
      (< ln (last-line pars par))    (Pos. par (inc ln) 0)
      (< par (last-par pars))        (Pos. (inc par) 0 0)
      :else                          pos)))

(defn chars-right [pos pars n]
  (loop [par (.-par pos), ln (.-ln pos), ch (.-ch pos), togo n]
    (let [last-ch (last-char pars par ln)
          last-ln (last-line pars par)]
      ; (log "Par: " par ", ln: " ln ", ch: " ch ", togo: " togo ", last-ch: " last-ch)
      (if (<= (+ ch togo) last-ch)
        (Pos. par ln (+ ch togo))
        (cond
          (< ln last-ln) (recur par (inc ln) 0 (- togo (- last-ch ch)))
          :else          (recur (inc par) 0 0 (- togo (- last-ch ch) 1)))))))

(defn move [$cur pos pars]
  (let [{:keys [par ln ch]} pos
        $p   (get-in pars [par :dom])
        top  (+ (.-top (.position $p)) (* ln *lh*))
        ch   (min ch (last-char pars par ln))
        left (* ch *chw*)]
    ; (log "Moving cursor to " par " " ln " " ch " (" top "px " left "px)")
    (jq/css $cur {:top (str top "px") :left (str left "px")})))

(defn repaint [pars]
  (move $cur-begin @cur-begin pars)
  (move $cur-end @cur-end pars))

(defn maybe-move-cursor [e pars]
  (let [processed (case (and e (.-which e))
                    37 (swap! cur-end char-left pars) ; ←
                    38 (swap! cur-end line-up pars)   ; ↑
                    39 (swap! cur-end char-right pars); →
                    40 (swap! cur-end line-down pars) ; ↓
                    nil)]
    (when (and processed (not (.-shiftKey e)))
      (reset! cur-begin @cur-end)
      :processed)))

(defn collapsed? []
  ; (log "Collapsed " (cmp @cur-begin @cur-end))
  (zero? (cmp @cur-begin @cur-end)))

(defn init []
  (let [$aux (jq/$ :#aux)
        text (.text $aux)]
    (set! *chw* (/ (.width $aux) (count text)))
    (jq/remove $aux)
    (log "Using char width: " *chw* "px")))
