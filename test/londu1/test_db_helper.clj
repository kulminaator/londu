(ns londu1.test-db-helper
  (:require
            [clojure.java.jdbc :as j]
            [londu1.test-db-credentials :as tdc]
            [londu1.operations.schema :as lschema]))

(defn execute-multiple-untransactional [db filename]
  (lschema/execute-multiple db (lschema/load-separator-defined-sql filename) true false))

(defn execute-multiple-transactional [db filename]
  (lschema/execute-multiple db (lschema/load-separator-defined-sql filename) true true))

(defn create-test-dbs[]
  ;; test that we can perform db actions at all
  (j/query tdc/source-db-creation-credentials
           ["select now();"])
  (j/query tdc/target-db-creation-credentials
           ["select now();"])
  ;;
  (execute-multiple-untransactional tdc/source-db-creation-credentials "test/test-db/drop-source-db.sql")
  (execute-multiple-untransactional tdc/target-db-creation-credentials "test/test-db/drop-target-db.sql")
  (execute-multiple-untransactional tdc/source-db-creation-credentials "test/test-db/create-source-db.sql")
  (execute-multiple-untransactional tdc/target-db-creation-credentials "test/test-db/create-target-db.sql"))

(defn create-test-structures[]
  (execute-multiple-transactional tdc/source-db-user-credentials "test/test-db/init-db.sql")
  (execute-multiple-transactional tdc/target-db-user-credentials "test/test-db/init-db.sql"))
