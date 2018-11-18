(ns londu1.core-test
  (:require [clojure.test :refer :all]
            [londu1.core :refer :all]
            [clojure.java.jdbc :as j]
            [londu1.test-db-credentials :as tdc]
            [londu1.operations.schema :as lschema]))

(defn create-test-dbs[]
  ;; test that we can perform db actions at all
  (j/query tdc/source-db-creation-credentials
           ["select now();"])
  (j/query tdc/target-db-creation-credentials
           ["select now();"])
  ;;
  (lschema/execute-multiple tdc/source-db-creation-credentials
                            (lschema/load-separator-defined-sql "test/test-db/drop-source-db.sql")
                            true
                            false)
  (lschema/execute-multiple tdc/target-db-creation-credentials
                            (lschema/load-separator-defined-sql "test/test-db/drop-target-db.sql")
                            true
                            false)

  (lschema/execute-multiple tdc/source-db-creation-credentials
                            (lschema/load-separator-defined-sql "test/test-db/create-source-db.sql")
                            true
                            false)

  (lschema/execute-multiple tdc/target-db-creation-credentials
                            (lschema/load-separator-defined-sql "test/test-db/create-target-db.sql")
                            true
                            false)

  )

(deftest test-data-sync
  (testing "Initial data from existing db is replicated to new db."
    (create-test-dbs)
    (is (= 0 1))))

