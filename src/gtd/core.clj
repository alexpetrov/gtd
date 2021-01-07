(ns gtd.core
  (:require [net.cgrand.enlive-html :as html]
            [datascript.core :as d]))

(def tmpl "templates/gtd.html")

(defn render [t]
  (reduce str t))

(html/deftemplate base tmpl
  [_]
)

(defn index []
  (->> []
       (base)
       (render)
       ))

(defn -main [& args]
  (println (index))
  (spit "resources/public/index.html" (index)))

(comment
  (-main)
  )
