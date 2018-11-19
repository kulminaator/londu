(ns londu1.core-test
  (:require [clojure.test :refer :all]
            [londu1.core :refer :all]
            [londu1.test-db-helper :as tdh]
            ))

(deftest test-data-sync
  (testing "Initial data from existing db is replicated to new db."
    (tdh/create-test-dbs)
    (is (= 0 1))))

