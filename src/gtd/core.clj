(ns gtd.core
  (:require [net.cgrand.enlive-html :as html]
            [datascript.core :as d]))

(defn entity [db id]
  (->> (d/entity db id)
       (into {})))

(defn db []
  (let [schema {:feature/id {:db/unique :db.unique/identity}
                :feature-kind/id {:db/unique :db.unique/identity}

                :feature/kind {:db/cardinality :db.cardinality/one :db/valueType :db.type/ref}
                :tune/features {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
                }
        conn (d/create-conn schema)]
    (d/transact! conn [{:db/id -100
                        :feature-kind/id "guitar"
                        :feature-kind/description "Kind of guitar"}

                       {:db/id -101
                        :feature-kind/id "guitarist"
                        :feature-kind/description "Musician playing on guitar in the tune"}

                       {:db/id -200
                        :feature/id "telecaster"
                        :feature/title "Telecaster"
                        :feature/kind [:feature-kind/id "guitar"]
                        :feature/description "First Leo Fender's solid body guitar"}

                       {:db/id -250
                        :feature/id "robben_ford"
                        :feature/title "Robben Ford"
                        :feature/kind [:feature-kind/id "guitarist"]
                        :feature/description "Robben Ford //TBD"}

                       {:db/id -251
                        :feature/id "gary_moore"
                        :feature/title "Gary Moore"
                        :feature/kind [:feature-kind/id "guitarist"]
                        :feature/description "Gary Moore //TBD"}

                       {:db/id -300
                        :tune/id "little_face"
                        :tune/title "Little Face"
                        :tune/band "David Sanborn"
                        :tune/youtube "https://www.youtube.com/embed/eGAgpn_m-ZE?start=170"
                        :tune/discogs "https://www.discogs.com/ru/David-Sanborn-Hearsay/release/2459059"
                        :tune/comment "First guitar tone I got obsessed with when I was 14"
                        :tune/features [[:feature/id "robben_ford"]
                                        [:feature/id "telecaster"]]}

                       {:db/id -400
                        :tune/id "blues_for_narada"
                        :tune/title "Blues For Narada"
                        :tune/band "Gary Moore"
                        :tune/youtube "https://www.youtube.com/embed/EntSKjMghnI"
                        :tune/comment "Second guitar tone I got obsessed with when I was 15"
                        :tune/features [[:feature/id "gary_moore"]]}] )

    @conn))

;; (-main)

(defn all-tunes [db]
  (->> (d/q '[:find ?id ?tid :where [?id :tune/id ?tid]] db)
       (map #(entity db (first %1)))
       (reverse)))

;; (all-tunes (db))

(defn render [t]
  (reduce str t))

(def tmpl "templates/gtd.html")

;; (html/deftemplate tune-features tmpl [:div.tune-features]
;;   [features]
;;   )

(html/defsnippet tune tmpl [:div.tune]
  [{:keys [tune/title tune/band tune/comment tune/youtube tune/discogs]}]
  [:.title] (html/html-content title)
  [:.band] (html/html-content band)
  [:.comment] (html/html-content comment)
  [:iframe.youtube] (html/set-attr :src youtube)
  [:.discogs] (if (nil? discogs) nil (html/set-attr :src discogs))
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

  (->> (all-tunes (db))
       (first)
       (:tune/features)
       (map #(entity (db) (:db/id %1)))
       )
  )
