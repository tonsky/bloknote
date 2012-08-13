(defproject bloknote "0.1.0-SNAPSHOT"
  :plugins        [[lein-cljsbuild "0.2.5"]
                   [lein-ring "0.7.1"]]
  :jvm-opts       ["-Dfile.encoding=UTF-8"]
  :cljsbuild {
    :builds {
      :dev {
        :source-path "src"
        :compiler {
          :output-to "resources/public/bloknote.js"
          :externs   ["externs/jquery.js"]
          :optimizations :whitespace
          :pretty-print  true}}
      :test {
        :source-path "test"
        :compiler {:output-to "resources/private/test/bloknote.js"
                   :optimizations :whitespace
                   :pretty-print  true}}
    }
    :test-commands
      ; Test command for running the unit tests in "test-cljs" (see below).
      ;     $ lein cljsbuild test
      {"unit" ["phantomjs"
               "resources/private/test/phantom.js"
               "resources/private/test/bloknote.html"]}
  }
  :ring         {:handler bloknote.server/app}
  :repl-init    bloknote.repl
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [jayq                "0.1.0-alpha4"]
                 [ring                "1.1.1"]
                 [compojure           "1.0.4"]
                 [hiccup              "1.0.0"]
                 ; [enfocus "0.9.1-SNAPSHOT"]
                ])
