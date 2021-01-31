(ns gtd.core
  (:require [net.cgrand.enlive-html :as html]
            [datascript.core :as d]
            [clojure.math.combinatorics :as combinatorics]))

(defn entity [db id]
  (->> (d/entity db id)
       (into {})))

(defn facts [] (read-string (slurp "resources/data/facts.edn")))
(defn schema [] (read-string (slurp "resources/data/schema.edn")))

(defn facts-with-ids [facts]
  (map
   (fn [[fact id]] (assoc fact :db/id (inc id)))
   (partition 2 (interleave facts (range)))))

(defn db []
  (let [conn (d/create-conn (schema))]
    (d/transact! conn (facts-with-ids (facts)))
    @conn))

;; (-main)

(def tune-with-features-pattern '[* {:tune/features [* {:feature/kind [*]}]}])
(defn tune-with-features [db id]
  (d/pull db tune-with-features-pattern id))

(defn all-tunes [db]
  (->> (d/q '[:find ?id ?tid :where [?id :tune/id ?tid]] db)
       (map #(tune-with-features db (first %1)))
       (reverse)))

(defn feature-data [db id]
  (d/pull db '[* {:feature/kind [*]}] id))

(defn all-features [db feature-ids]
  (map #(feature-data db %1) feature-ids))

(defn all-features-ids [db]
  (->> (d/q '[:find ?id ?fid :where [?id :feature/id ?fid]] db)
       (map first)))

;; (all-features-ids (db))
;; (all-features (db) (all-features-ids (db)))

(defn feature [db feature-kind-id tune-id]
  (let [feature-db-ids (d/q '[:find ?feature-id
                              :in $ ?fkid ?tid
                              :where
                              [?tid :tune/features ?feature-id]
                              [?feature-id :feature/kind ?fkid]
                              ]
                            db [:feature-kind/id feature-kind-id] [:tune/id tune-id])]
    (d/pull db '[* {:feature/kind [*]}] (ffirst feature-db-ids))))



;; (def test-data [{:db/id 5, :feature/description "Gary Moore //TBD", :feature/id "gary_moore", :feature/kind {:db/id 2, :feature-kind/description "Musician playing on guitar in the tune", :feature-kind/id "guitarist"}, :feature/title "Gary Moore"}])

;; (all-tunes (db))
;;(:tune/features (first (all-tunes (db))))
;;
;; (d/pull (db) '[:feature/title {:feature/kind [:db/id :feature-kind/title]}] [:feature/id "telecaster"])
;; (d/pull (db) '[:tune/title :tune/id {:tune/features [* {:feature/kind [*]}]}] [:tune/id "little_face"])


(defn render [t]
  (reduce str t))

(def tmpl "templates/gtd.html")

(html/defsnippet tune-feature tmpl [:span.tune-feature]
  [{:keys [feature/title feature/description feature/kind]}]
  [:span.tune-feature-kind] (html/content (:feature-kind/title kind))
  [:span.tune-feature-value] (html/content title)
  [:span.tune-feature-value] (html/set-attr :title description)
  )

(html/defsnippet tune-features tmpl [:div.tune-features]
  [tune-id]
  [:tr.guitarist] (html/content (tune-feature (feature (db) "guitarist" tune-id)))
  [:tr.guitar] (html/content (tune-feature (feature (db) "guitar" tune-id)))
  )

(html/defsnippet tune tmpl [:div.tune]
  [{:keys [tune/title tune/band tune/comment tune/youtube tune/discogs tune/features tune/id]}]
  [:span.title] (html/content title)
  [:span.band] (html/content band)
  [:div.comment] (html/content comment)
  [:iframe.youtube] (html/set-attr :src youtube)
  [:a.discogs] (if (nil? discogs) nil (html/set-attr :href discogs))
  [:div.tune-features] (html/content (map tune-feature features))
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
  (->> [1 2 3]
       (combinatorics/subsets)
       (remove empty?)
       (map set)
       (set))


  )
