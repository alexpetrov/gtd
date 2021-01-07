(defproject gtd "0.1.0-SNAPSHOT"
  :description "Guitar Tone Discovery site generator"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [datascript "1.0.3"]
                 [enlive "1.1.6"]]
  :main gtd.core
  :repl-options {:init-ns gtd.core})
