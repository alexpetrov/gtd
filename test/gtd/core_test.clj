(ns gtd.core-test
  (:require [clojure.test :refer :all]
            [gtd.core :refer :all]))

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

(defn features-objects-tree [features objects]
  [])

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
