(ns bloknote.sheet
  (:require [jayq.core :as jq]
            [clojure.string :as str]
            [bloknote.utils :as u]
            [bloknote.cursor :as cur]
            [bloknote.edit :as edit]
            [cljs.reader :as reader])
  (:use [jayq.util :only [log]]))

(def undo_limit 50)

(def $sheet  (jq/$ :#sheet))
(def $titles (jq/$ "#titles"))
(def pars    (atom []))
(def user    (atom ""))

(defn on-type [e]
  (if (or (cur/maybe-move-cursor e @pars)
          (edit/maybe-insert e pars))
    (do
      (cur/repaint @pars)
      (jq/prevent e))
    (do
      (js/setTimeout 
        (fn [] 
          (edit/maybe-insert pars)
          (cur/repaint @pars))
        20))))

(defn render-titles [titles last-opened-id]
  (doseq [[title id] titles]
    (let [selected (if (= id last-opened-id) " selected" "")
          $li      (jq/$ (str "<li id='title-" id "' class='title" selected "'>" title "</li>"))]
      (.append $titles $li)
      (.data $li "bloknoteid" id)))
  (.append $titles "<li class='title-new'>[ + Add Bloknote ]</li>"))

(defn clear-sheet [text]
  (jq/remove (jq/$ "#sheet p")))
  ; (reset! pars (edit/split-to-pars text))
  ; (doseq [par @pars]
  ;   (jq/add-class (:dom par) "aux")
  ;   (jq/append $sheet (:dom par)))
  ;   (reset! cur/cur-begin (cur/Pos. 0 0 (count text)))
  ;   (reset! cur/cur-end   (cur/Pos. 0 0 (count text)))
  ;   (cur/repaint @pars))

(defn init-sheet [b id]
  (let [{:keys [text cur-begin cur-end]} b]
    (log "loaded " text)
    (reset! pars (edit/split-to-pars text))
    (jq/remove (jq/$ "#sheet .aux"))
    (doseq [par @pars]
      (jq/append $sheet (:dom par)))
    (let [from (cur/Pos. (cur-begin 0) (cur-begin 1) (cur-begin 2))
          to   (cur/Pos. (cur-end 0)   (cur-end 1)   (cur-end 2))]
      (reset! cur/cur-begin from)
      (reset! cur/cur-end to)
      (cur/repaint @pars))
    (let [title (first (str/split text #"\n" 2))]
      (log "title: " title ", id: #title-" id)
      (jq/text (jq/$ (str "#title-" id)) title))
    (.focus edit/$ta)))

(defn load-sheet [u sheet]
  (clear-sheet "Loading...")
  (jq/ajax
    (str "/api/" u "/" sheet "/")
    {:success (fn [data] (-> data
                             reader/read-string
                             (init-sheet sheet)))}))

(defn on-title-click [e]
  (let [$t (jq/$ (.-target e))]
    (when-let [id (.data $t "bloknoteid")]
      (jq/remove-class (jq/$ "#titles > .selected") "selected")
      (jq/add-class $t "selected")
      (load-sheet @user id))
    (when (jq/has-class $t "title-new")
      (let [id (str (rand-int 1000000000))
            s  (str "<li class='title' id='title-" id "'>New one</li>")
            $li (jq/$ s)]
        (jq/data $li "bloknoteid" id)
        (jq/before $t $li)
        (.click $li)))))

(defn init-user [u]
  (reset! user u)
  (cur/init)
  (edit/init)
  (jq/bind edit/$ta :keydown on-type)
  (jq/bind $sheet :click (fn [_] (.focus edit/$ta)))
  (jq/bind edit/$ta :focus (fn [_] (jq/add-class $sheet :focused)))
  (jq/bind edit/$ta :blur  (fn [_] (jq/remove-class $sheet :focused)))
  ; (jq/bind (jq/$ :body) :keyup on-type)
  (jq/bind $titles :click on-title-click)
  (jq/ajax
    (str "/api/" u "/")
    {:success (fn [data] 
                (let [data (reader/read-string data)
                      {:keys [titles last-opened-id last-opened]} data]
                  (render-titles titles last-opened-id)
                  (init-sheet last-opened last-opened-id)))}))
                  
