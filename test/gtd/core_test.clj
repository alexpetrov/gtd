(ns gtd.core-test
  (:require [clojure.test :refer :all]
            [gtd.core :refer :all]
            [clojure.zip :as zip]))

(def features #{"a" "b" "c"})

(def objects ["ab" "ac" "bc" "abc"])

(defn object-has-feature [object feature]
  (clojure.string/includes? object feature))
(defn objects-with-features [objects features]
  (into (empty objects)
        (filter
         (fn [object] (every? #(object-has-feature object %) features)) objects)))

(defn get-children [path features objects]
  (let [rest-features (clojure.set/difference features path)]
    (into [] (map (fn [feature] {:feature feature :objects (objects-with-features objects (conj path feature))}) rest-features))))

(defn get-children-nodes []
  (fn [node]
    (let [path (:path node)
          _ (println path)]
      (get-children (:path node) features objects)))
  )

(defn features-objects-zipper [root_feature features objects]
  (zip/zipper (constantly true) ;; FIXME: children? must not be constantly true, because at least at some oponint there will be no features left in the set
              get-children-nodes
              (fn [n children] (assoc n :children children))
              root_feature)) ;; FIXME: it turns out, that we cannot solve a problem with dynamic tree. Tree must be static and only zipper have to be used to traverse it and to build a visual tree.

(comment
  (-> (features-objects-zipper "a" features objects)
      (z/node)))

(defn features-objects-tree [root_feature features objects])

(defn features-objects-forest [features objects]
  (map features))

(deftest test-get-children
  (is (= [{:feature "a", :objects ["ac" "abc"]} {:feature "b", :objects ["bc" "abc"]}]
         (get-children #{"c"} features objects)))
  (is (= [{:feature "a", :objects ["ab" "abc"]} {:feature "c", :objects ["bc" "abc"]}]
         (get-children #{"b"} features objects)))
  (is (= [{:feature "b", :objects ["ab" "abc"]} {:feature "c", :objects ["ac" "abc"]}]
         (get-children #{"a"} features objects)))
  (is (= [{:feature "c", :objects ["abc"]}]
         (get-children #{"a" "b"} features objects))))

(deftest test-object-has-feature
  (is (true? (object-has-feature "ab" "a"))))

(deftest test-objects-with-features
  (is (= ["a" "ab" "ac"] (objects-with-features ["a" "b" "ab" "ac"] ["a"])))
  (is (= ["ab"] (objects-with-features ["a" "b" "ab" "ac"] ["a" "b"])))
  (is (= ["b" "ab"] (objects-with-features ["a" "b" "ab" "ac"] ["b"])))
  (is (= ["ac"] (objects-with-features ["a" "b" "ab" "ac"] ["c"]))))

(deftest name-it-after-it-is-done
  (let [features ["a" "b" "c"]
        objects ["a" "b" "ab" "bc"]]
    (is (= [{:feature "a"
             :objects ["a" "ab"]
             :children [{:feature "b" :objects ["ab"]}]
             }
            {:feature "b"
             :objects ["b" "ab" "bc"]
             :children [{:feature "a" :objects ["ab"]}
                        {:feature "c" :objects ["bc"]}]}
            {:feature "c"
             :objects ["bc"]
             :children [{:feature "b" :objects ["bc"]}]}
            ]
           (features-objects-tree features objects)))))
