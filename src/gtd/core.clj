(ns gtd.core
  (:require [net.cgrand.enlive-html :as html]
            [datascript.core :as d]
            [clojure.math.combinatorics :as combinatorics]))

(defn dot-html [s]
  (str s ".html"))

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
;; FIXME: rename to tune-data or something else
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

(defn feature-subsets [db]
  (->> (all-features-ids db)
       (combinatorics/subsets)
       (remove empty?)
       (map set)
       (set)))

;; (feature-subsets (db))
;; (all-features-ids (db))
;; (all-features (db) (all-features-ids (db)))

(defn tunes-with-some-features [db feature-ids]
  (d/q '[:find (count ?id) (distinct ?id)
         :in $ [?fid ...]
         :where
         [?id :tune/features ?fid]]
       db feature-ids))

;; (tunes-with-some-features (db) (all-features-ids (db)))

(defn all-features-present? [feature-ids tune]
  (let [tune-feature-ids-set (set (map :db/id (:tune/features tune)))
        feature-ids-set (set feature-ids)]
    (empty? (clojure.set/difference feature-ids-set tune-feature-ids-set))))

;; (clojure.set/difference #{1 2} #{1 3 4})

;; (all-features-present? '(6) (first (all-tunes (db))))
(defn tunes-with-all-features [db feature-ids]
  (->> (all-tunes db)
       (filter #(all-features-present? feature-ids %))))

;; (count (tunes-with-all-features (db) '(5 3)))
;; (all-features-ids (db)) (6 3 4 5)
;; (tunes-with-all-features (db) '(6 4 3 5))

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
  [{:keys [feature/title feature/description feature/kind feature/id]}]
  [:a.tune-feature-link] (html/set-attr :href (dot-html id))
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

(defn generate-page-for-tunes-with-feature-set [db tunes feature-set-db-ids]
  (let [features (all-features db feature-set-db-ids)
        feature-ids (map :feature/id features)
        feature-names-joined (clojure.string/join "-" feature-ids)
        url (str "public/" (dot-html feature-names-joined))
        file-name (str "resources/" url)
        page-source (->> tunes (base) (render))]
    (spit file-name page-source)
    [feature-names-joined (count tunes)]))

(defn generate-page-for-feature-set [db feature-set-db-ids]
  (let [tunes (tunes-with-all-features db feature-set-db-ids)]
    (if (empty? tunes)
      nil
      (generate-page-for-tunes-with-feature-set db tunes feature-set-db-ids))))

(defn generate-pages-for-all-feature-sets []
  (->> (feature-subsets (db))
       (map #(generate-page-for-feature-set (db) %))))

;; (generate-pages-for-all-feature-sets)

(defn -main [& args]
  (spit "resources/public/index.html" (index))
  (generate-pages-for-all-feature-sets))

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
