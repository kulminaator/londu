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


(defn create-test-data[]
  (j/execute! tdc/source-db-user-credentials
           ["insert into shop_items(name, price) values ('10 buck shirt', 10)"])
  (j/execute! tdc/source-db-user-credentials
           ["insert into shop_items(name, price) values ('2.5 buck jeans', 2.50)"])
  (j/execute! tdc/source-db-user-credentials
           ["insert into shop_workers(name) values ('bob the builder')"]))

(defn prepare-replication-env[]
  (lschema/create-schema-and-triggers tdc/source-db-user-credentials)
  (lschema/create-schema-and-triggers tdc/target-db-user-credentials))

(defn add-test-data[]
  (j/execute! tdc/source-db-user-credentials
           ["insert into shop_items(name, price) values ('25 buck car', 25)"])
  (j/execute! tdc/source-db-user-credentials
           ["insert into shop_items(name, price) values ('.50 cent', 0.50)"])
  (j/execute! tdc/source-db-user-credentials
           ["insert into shop_workers(name) values ('janice the manager')"]))

(defn source-op[a-function]
  (a-function tdc/source-db-user-credentials))

(defn get-all-shop-items[db]
  (j/query db "SELECT * FROM shop_items order by id, name, price"))

(defn get-all-shop-workers[db]
  (j/query db "SELECT * FROM shop_workers order by id, name"))


(def source-db tdc/source-db-user-credentials)
(def target-db tdc/target-db-user-credentials)