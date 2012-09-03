(ns bloknote.test-editor
  (:use [bloknote.editor :only [par-style split-to-tags Tag]]))

(defn test-par-style []
  (.log js/console "Testing test-par-style")

  ; Headers
  (let [s (par-style "### Header")]
    (assert (= (:style s) :header))
    (assert (= (:offset s) 0))
    (assert (= (:first-line-offset s) -4)))
  ; Header does not fit to 7-symbol margin
  (let [s (par-style "####### Header")]
    (assert (= (:style s) :header))
    (assert (= (:first-line-offset s) -7)))
  (let [s (par-style " ### not-a-header")]
    (assert (= (:style s) :text))
    (assert (= (:offset s) 0))
    (assert (= (:first-line-offset s) 0)))
  ; Blockquotes
  (let [s (par-style "> blockquote")]
    (assert (= (:style s) :blockquote))
    (assert (= (:offset s) 2))
    (assert (= (:first-line-offset s) 0)))
  ; Lists
  (let [s (par-style "*** list")]
    (assert (= (:style s) :ul))
    (assert (= (:offset s) 0))
    (assert (= (:first-line-offset s) -4)))
  (let [s (par-style "1. list")]
    (assert (= (:style s) :ol))
    (assert (= (:offset s) 0))
    (assert (= (:first-line-offset s) -3)))
  (let [s (par-style "1.2. list")]
    (assert (= (:style s) :ol))
    (assert (= (:first-line-offset s) -5)))
  (let [s (par-style "12.13.14. list")]
    (assert (= (:style s) :ol))
    (assert (= (:first-line-offset s) -7)))
  (let [s (par-style "12.13 list")]
    (assert (= (:style s) :text)))
  (let [s (par-style "12.13.list")]
    (assert (= (:style s) :text))))

(defn- tag-to-vec [tag]
  (cond
    (:attrs tag)          [(:name tag) (:text tag) (:attrs tag)]
    (= (:name tag) :span) (:text tag)
    :else                 [(:name tag) (:text tag)]))

(defn- split-to-vec [text]
  (map tag-to-vec (split-to-tags text)))

(defn test-split-to-tags []
  (.log js/console "Testing test-split-to-tags")
  (assert (= (split-to-vec "How *are* you?")
             ["How " "*" [:em "are"] "*" " you?"]))
  ; Some edge cases
  (assert (= (split-to-vec "*How* are *you?* or*not you* in*the*word *end*")
             ["*" [:em "How"] "*" " are " "*" [:em "you?"] "*" " or*not you* in*the*word " "*" [:em "end"] "*"]))
  ; Same for 'strong' tag
  (assert (= (split-to-vec "**How** are **you?** or**not you** in**the**word **end**")
             ["**" [:strong "How"] "**" " are " "**" [:strong "you?"] "**" " or**not you** in**the**word " "**" [:strong "end"] "**"]))
  ; Nesting stuff, *strong* preceeds *em*
  (assert (= (split-to-vec "**strong *italic* strong** span *italic **strong** italic*")
             ["**" [:strong "strong "] [:strong "*"] [:em "italic"] [:strong "*"] [:strong " strong"] "**" " span *italic " "**" [:strong "strong"] "**" " italic*"]))
  ; URLs
  (assert (= (split-to-vec "Do you mean www.ya.ru:8080?")
             ["Do you mean " [:a "www.ya.ru:8080" {:href "http://www.ya.ru:8080"}] "?"]))
  ; Nesting URLs
  (assert (= (split-to-vec "*em http://twitter.com/* and **strong https://facebook.ru**")
             ["*" [:em "em "] [:a "http://twitter.com/" {:href "http://twitter.com/"}] "*" " and " "**" [:strong "strong "] [:a "https://facebook.ru" {:href "https://facebook.ru"}] "**"])))

(defn ^:export run []
  (test-par-style)
  (test-split-to-tags)
  0)

