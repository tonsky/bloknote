(ns bloknote.sheet
  (:require [jayq.core :as jq]
            [clojure.string :as str]
            [bloknote.utils :as u]
            [bloknote.cursor :as cur]
            [bloknote.edit :as edit])
  (:use [jayq.util :only [log]]))

(def undo_limit 50)

(def $sheet (jq/$ :#sheet))
(def pars   (atom []))

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

(defn init [text]
  (reset! pars (edit/split-to-pars text))
  (doseq [par @pars]
    (jq/append $sheet (:dom par)))
  (cur/init @pars)
  (edit/init @pars)
  (jq/bind edit/$ta :keydown on-type)
  (jq/bind $sheet :click (fn [_] (.focus edit/$ta)))
  (jq/bind edit/$ta :focus (fn [_] (jq/add-class $sheet :focused)))
  (jq/bind edit/$ta :blur  (fn [_] (jq/remove-class $sheet :focused)))
  ; (jq/bind (jq/$ :body) :keyup on-type)
  )
