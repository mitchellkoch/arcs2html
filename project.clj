(defproject arcs2html "0.1.0-SNAPSHOT"
  :description "Convert ARCs to HTML"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.jwat/jwat-arc "0.9.0"]]
  :jvm-opts ["-Xmx512m" "-server"]
  :main arcs2html.core)
