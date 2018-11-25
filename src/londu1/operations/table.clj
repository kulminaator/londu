(ns londu1.operations.table
  (:gen-class)
  (:use [londu1.operations.json :only [unjson]])
  (:require [clojure.java.jdbc :as j]))


(defn compose-declare-read-cursor [qtablename]
  ;; todo aside from normalizing the table name should also validate that it can be a table name at all.
  ;; it may carry special symbols so will double quote it anyway here
  (let [safe-tablename (clojure.string/replace qtablename #"[^a-zA-Z0-9_]" "_")
        [schema tablename] (clojure.string/split qtablename #"\.")]
    (str "DECLARE __londu_1_cursor_" safe-tablename
         " CURSOR WITHOUT HOLD FOR SELECT row_to_json(\"" schema "\".\"" tablename "\".*)::text "
         " AS nd FROM \"" schema "\".\"" tablename "\"")
    )
  )

(defn compose-close-read-cursor [qtablename]
  ;; todo aside from normalizing the table name should also validate that it can be a table name at all.
  ;; it may carry special symbols so will double quote it anyway here
  (let [safe-tablename (clojure.string/replace qtablename #"[^a-zA-Z0-9_]" "_")]
    (str "CLOSE __londu_1_cursor_" safe-tablename)
    )
  )

(defn copy-table-data [x-src-db x-tgt-db qtablename]
  (let [safe-tablename (clojure.string/replace qtablename #"[^a-zA-Z0-9_]" "_")]
    (loop [qr (j/query x-src-db [(str "FETCH FORWARD 100 FROM __londu_1_cursor_" safe-tablename)])]
      ;;(doseq [row qr] (j/insert! x-tgt-db (str qtablename) (unjson (:nd row))))
      (let [unjsoned-rows (map #(unjson (:nd %)) qr)]
        (j/insert-multi! x-tgt-db (str qtablename) unjsoned-rows))
      (when-not (empty? qr)
        (recur (j/query x-src-db [(str "FETCH FORWARD 10 FROM __londu_1_cursor_" safe-tablename)])))
      )))

(defn table-copy [qtablename x-src-db x-tgt-db]
  "Copies a table (with fully qualified name in schema.tablename format from source to target."
  (let [[schema table] (clojure.string/split qtablename #"\.")]
    (j/execute! x-src-db(compose-declare-read-cursor (str schema "." table)))
    (copy-table-data x-src-db x-tgt-db (str schema "." table))
    (j/execute! x-src-db(compose-close-read-cursor (str schema "." table)))
    ))
