(ns londu1.operations.schema
  (:gen-class)
  (:require [clojure.java.jdbc :as j])
  )

(defn load-separator-defined-sql[file-or-stream]
  (let [sourcecode (slurp file-or-stream)
        statements (clojure.string/split sourcecode #"-- separator --")]
    statements))

(defn execute-multiple [db statements verbose with-transaction]
  (doseq [statement statements]
    (when verbose (println (str "multi-exec:" statement)))
    (j/execute! db statement {:transaction? with-transaction})))

(defn compose-create-schema-tables-and-triggers[]
  (load-separator-defined-sql (clojure.java.io/resource "londu1-tables-and-triggers.sql")))
;; (compose-create-schema-and-triggers)

(defn create-schema-and-triggers [db]
  (execute-multiple (compose-create-schema-tables-and-triggers) false true))
;; (create-schema-and-triggers londu1.core/pg-source-db)