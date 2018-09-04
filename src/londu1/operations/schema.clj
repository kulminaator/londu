(ns londu1.operations.schema
  (:gen-class)
  (:require [clojure.java.jdbc :as j])
  )

(defn compose-create-schema-tables-and-triggers[]
  (let [sourcecode (slurp (clojure.java.io/resource "londu1-tables-and-triggers.sql"))
        statements (clojure.string/split sourcecode #"-- separator --")]
    statements))
;; (compose-create-schema-and-triggers)

(defn create-schema-and-triggers [db]
  (doseq [statement (compose-create-schema-tables-and-triggers)]
    (j/execute! db statement)))

;; (create-schema-and-triggers londu1.core/pg-source-db)