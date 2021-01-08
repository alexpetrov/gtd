(ns gtd.core
  (:require [net.cgrand.enlive-html :as html]
            [datascript.core :as d]))

(defn entity [db id]
  (->> (d/touch (d/entity db id))
       (into {})))

(defn db []
  (let [schema {}
        conn (d/create-conn schema)]
    (d/transact! conn [ {:db/id -1
                         :tune/title "Little Face"
                         :tune/band "David Sanborn"
                         :tune/youtube "https://www.youtube.com/embed/eGAgpn_m-ZE?start=170"
                         ;; :tune/youtube "https://www.youtube.com/embed/eGAgpn_m-ZE?start=170"
                         :tune/comment "First guitar tone I got obsessed with when I was 14"}

                       {:db/id -2
                        :tune/title "Blues For Narada"
                        :tune/band "Gary Moore"
                        :tune/youtube "https://www.youtube.com/embed/EntSKjMghnI"
                        :tune/comment "Second guitar tone I got obsessed with when I was 15"}] )

    @conn))

(defn all-tunes [db]
  (->> (d/q '[:find ?id :where [?id _ _]] db)
       (map #(entity db (first %1)))
       ))


(defn render [t]
  (reduce str t))

(def tmpl "templates/gtd.html")

(html/defsnippet tune tmpl [:div.tune]
  [tune]
  [:.title] (html/html-content (:tune/title tune))
  [:.band] (html/html-content (:tune/band tune))
  [:.comment] (html/html-content (:tune/comment tune))
  [:iframe.youtube] (html/set-attr :src (:tune/youtube tune))
  )

;; (-main)

(html/deftemplate base tmpl
  [tunes]
  [:div#inner-content] (html/content (map tune tunes))
)

(defn index []
  (->> (all-tunes (db))
       (base)
       (render)
       ))

(defn -main [& args]
  (println (index))
  (spit "resources/public/index.html" (index)))

(comment
  (-main)
  (all-tunes (db))
  (d/q '[:find ?id :where [?id _ _]] (db))
  )
