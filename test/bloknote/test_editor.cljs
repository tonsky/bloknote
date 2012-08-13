(ns bloknote.test-editor
  (:use [bloknote.editor :only [par-style split-to-tags Tag]]))

(defn test-par-style []
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

(defn test-split-to-tags []
  (let [tags (split-to-tags "How *are* you?")
        [t1 t2 t3 t4 t5] tags]
    (assert (= (Tag. "How "  :span nil) t1))
    (assert (= (Tag. "*"     :span nil) t2))
    (assert (= (Tag. "are"   :em   nil) t3))
    (assert (= (Tag. "*"     :span nil) t4))
    (assert (= (Tag. " you?" :span nil) t5)))
  ; Some edge cases
  (let [tags (split-to-tags "*How* are *you?* or*not you* in*the*word *end*")
        [t1 t2 t3 t4 t5 t6 t7 t8 t9 t10 t11] tags]
    (assert (= (Tag. "*"       :span nil) t1))
    (assert (= (Tag. "How"     :em nil)   t2))
    (assert (= (Tag. "*"       :span nil) t3))
    (assert (= (Tag. " are "   :span nil) t4))
    (assert (= (Tag. "*"       :span nil) t5))
    (assert (= (Tag. "you?"    :em   nil) t6))
    (assert (= (Tag. "*"       :span nil) t7))
    (assert (= (Tag. " or*not you* in*the*word " :span nil) t8))
    (assert (= (Tag. "*"       :span nil) t9))
    (assert (= (Tag. "end"     :em   nil) t10))
    (assert (= (Tag. "*"       :span nil) t11)))
  ; Same for 'strong' tag
  (let [tags (split-to-tags "**How** are **you?** or**not you** in**the**word **end**")
        [t1 t2 t3 t4 t5 t6 t7 t8 t9 t10 t11] tags]
    (assert (= (Tag. "**"      :span   nil) t1))
    (assert (= (Tag. "How"     :strong nil) t2))
    (assert (= (Tag. "**"      :span   nil) t3))
    (assert (= (Tag. " are "   :span   nil) t4))
    (assert (= (Tag. "**"      :span   nil) t5))
    (assert (= (Tag. "you?"    :strong nil) t6))
    (assert (= (Tag. "**"      :span   nil) t7))
    (assert (= (Tag. " or**not you** in**the**word " :span nil) t8))
    (assert (= (Tag. "**"      :span   nil) t9))
    (assert (= (Tag. "end"     :strong nil) t10))
    (assert (= (Tag. "**"      :span   nil) t11)))
  ; Nesting stuff, *strong* preceeds *em*
  (let [tags (split-to-tags "**strong *italic* strong** span *italic **strong** italic*")
        [t1 t2 t3 t4 t5 t6 t7 t8 t9 t10 t11 t12] tags]
    (assert (= (Tag. "**"             :span   nil) t1))
    (assert (= (Tag. "strong "        :strong nil) t2))
    (assert (= (Tag. "*"              :strong nil) t3))
    (assert (= (Tag. "italic"         :em     nil) t4))
    (assert (= (Tag. "*"              :strong nil) t5))
    (assert (= (Tag. " strong"        :strong nil) t6))
    (assert (= (Tag. "**"             :span   nil) t7))
    (assert (= (Tag. " span *italic " :span   nil) t8))
    (assert (= (Tag. "**"             :span   nil) t9))
    (assert (= (Tag. "strong"         :strong nil) t10))
    (assert (= (Tag. "**"             :span   nil) t11))
    (assert (= (Tag. " italic*"       :span   nil) t12)))
  ; URLs
  (let [tags (split-to-tags "Do you mean www.ya.ru:8080?")
        [t1 t2 t3] tags]
    (assert (= (Tag. "Do you mean "   :span nil) t1))
    (assert (= (Tag. "www.ya.ru:8080" :a {:href "http://www.ya.ru:8080"}) t2))
    (assert (= (Tag. "?"              :span nil) t3)))
  ; Nesting URLs
  (let [tags (split-to-tags "*em http://twitter.com/* and **strong https://facebook.ru**")
        [t1 t2 t3 t4 t5 t6 t7 t8 t9] tags]
    (assert (= (Tag. "*"       :span nil)   t1))
    (assert (= (Tag. "em "     :em   nil)   t2))
    (assert (= (Tag. "http://twitter.com/" :a {:href "http://twitter.com/"}) t3))
    (assert (= (Tag. "*"       :span nil)   t4))
    (assert (= (Tag. " and "   :span nil)   t5))
    (assert (= (Tag. "**"      :span nil)   t6))
    (assert (= (Tag. "strong " :strong nil) t7))
    (assert (= (Tag. "https://facebook.ru" :a {:href "https://facebook.ru"}) t8))
    (assert (= (Tag. "**"      :span nil) t9))))

(defn ^:export run []
  (.log js/console "Testing test-par-style")
  (test-par-style)
  (.log js/console "Testing test-split-to-tags")
  (test-split-to-tags)
  0)

