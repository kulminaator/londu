(ns londu1.core-test
  (:require [clojure.test :refer :all]
            [londu1.core :refer :all]
            [londu1.test-db-helper :as tdh]))

(deftest test-data-sync
  (testing "Initial data from existing db is replicated to new db."
           (tdh/create-test-dbs)
           (tdh/create-test-structures)
           (tdh/create-test-data)
           (tdh/prepare-replication-env)
           (add-table-to-replication pg-source-db pg-target-db "public.shop_items")
           (add-table-to-replication pg-source-db pg-target-db "public.shop_workers")
           (tdh/add-test-data)
           (tdh/modify-some-test-data)
           (tdh/delete-some-test-data)
           (create-tick tdh/source-db)
           (replicate-step tdh/source-db tdh/target-db nil)
           (let [source-item-data (tdh/get-all-shop-items tdh/source-db)
                 target-item-data (tdh/get-all-shop-items tdh/target-db)]
             (is (= source-item-data target-item-data)))
           (let [source-worker-data (tdh/get-all-shop-workers tdh/source-db)
                 target-worker-data (tdh/get-all-shop-workers tdh/target-db)]
             (is (= source-worker-data target-worker-data)))
           ))

