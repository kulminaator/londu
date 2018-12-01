(ns londu1.operations.table
  (:gen-class)
  (:use [londu1.operations.json :only [from-json to-json]])
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

(defn effective-multi-insert-old
  "Tries to perform a really effective multi row insert, using json decoded values from clojure and insert-multi!"
  [db table rows]
  (when (not-empty rows)
    (let [row-keys (keys (first rows))
          rows-values (map vals rows)]
      (println row-keys)
      (println rows-values)
      (j/insert-multi! db table row-keys rows-values {})
      )))


(defn effective-multi-insert
  "Tries to perform a really effective multi row insert, but uses db's own json decoding to do so."
  [db qtablename rows]
  (when (not-empty rows)
    (let [[schema tablename] (clojure.string/split qtablename #"\.")
          jsonified-rows (to-json rows)
          sql-statement [(str
                         "INSERT INTO \"" schema "\".\"" tablename "\" "
                         "  SELECT * FROM json_populate_recordset("
                          "   NULL::\"" schema "\".\"" tablename "\", "
                              "?::json"
                            ")") jsonified-rows]]
      (println (str "! " sql-statement))
      (j/execute! db sql-statement {})
      )))

(defn copy-table-data [x-src-db x-tgt-db qtablename]
  (let [safe-tablename (clojure.string/replace qtablename #"[^a-zA-Z0-9_]" "_")]
    (loop [qr (j/query x-src-db [(str "FETCH FORWARD 100 FROM __londu_1_cursor_" safe-tablename)])]
      ;;(doseq [row qr] (j/insert! x-tgt-db (str qtablename) (from-json(:nd row))))
      (let [unjsoned-rows (map #(from-json(:nd %)) qr)]
        (effective-multi-insert x-tgt-db qtablename unjsoned-rows))
      (when-not (empty? qr)
        (recur (j/query x-src-db [(str "FETCH FORWARD 100 FROM __londu_1_cursor_" safe-tablename)])))
      )))

(defn table-copy [qtablename x-src-db x-tgt-db]
  "Copies a table (with fully qualified name in schema.tablename format from source to target."
  (let [[schema table] (clojure.string/split qtablename #"\.")]
    (j/execute! x-src-db(compose-declare-read-cursor (str schema "." table)))
    (copy-table-data x-src-db x-tgt-db (str schema "." table))
    (j/execute! x-src-db(compose-close-read-cursor (str schema "." table)))))

(defn compose-create-trigger [qtablename]
  ;; todo aside from normalizing the table name should also validate that it can be a table name at all.
  ;; it may carry special symbols so will double quote it anyway here
  (let [safe-tablename (clojure.string/replace qtablename #"[^a-zA-Z0-9_]" "_")
        [schema tablename] (clojure.string/split qtablename #"\.")]
    (str "CREATE TRIGGER __londu_1_trigger_" safe-tablename
         " BEFORE INSERT OR UPDATE OR DELETE ON \"" schema "\".\"" tablename "\""
         " FOR EACH ROW EXECUTE PROCEDURE __londu_1.trigger();")))

(defn create-trigger
  "Creates the __londu_1 trigger on the table on the specified database to collect insert update delete events."
  [x-db-con qtablename]
  (j/execute! x-db-con (compose-create-trigger qtablename)))
