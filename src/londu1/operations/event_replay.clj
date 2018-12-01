(ns londu1.operations.event-replay
  (:gen-class)
  (:use [londu1.operations.json :only [from-json to-json]])
  (:require [clojure.java.jdbc :as j]))

(defn quote-schemed-tablename
  "Turns foo bar into \"foo\".\"bar\" to use them safely as qualified table names around the sql.
  Removes any double quotes from input."
  [schema tablename]
  (let [cleaned-schema (clojure.string/replace schema #"\"" "")
        cleaned-tablename (clojure.string/replace tablename #"\"" "")]
    (str "\"" cleaned-schema "\".\"" cleaned-tablename "\"")))

(defn replay-insert-in-target [event x-tgt-db]
  (let [schema (:s event)
        table (:t event)
        key-values (from-json (:nd event))]

    (let [sqltablename (quote-schemed-tablename schema table)
          jsonified-data (to-json key-values)
          sql-statement [(str
                          "INSERT INTO " sqltablename
                          "  SELECT * FROM json_populate_record(NULL::" sqltablename ",?::json)") jsonified-data]]
      (j/execute! x-tgt-db sql-statement {}))
    ))

(defn build-where-str [key-values]
  (let [ks (keys key-values)]
    (clojure.string/join " AND " (map #(str "\"" % "\" = ?") ks))
    ))

(defn replay-delete-in-target [event x-tgt-db]
  (let [schema (:s event)
        table (:t event)
        old-key-values (from-json (:od event))
        del-values (vec (cons (build-where-str old-key-values) (vals old-key-values)))]
    ; (println (str "Deleting " old-key-values))
    (j/delete! x-tgt-db (str schema "." table) del-values)
    ))

(defn safe-double-quote
  [text-to-secure]
  (str "\"" (clojure.string/replace text-to-secure #"\"" "") "\""))

(defn build-fields-list-for-update
  [new-data]
  (clojure.string/join "," (map safe-double-quote (keys new-data))))

(defn update-condition-for-key
  [sql-tablename key]
  (let [lefty (str (safe-double-quote "__londu_old_data") "." (safe-double-quote key))
        righty (str sql-tablename "." (safe-double-quote key))]
      (str "(" lefty " = " righty " OR (" lefty " IS NULL AND " righty " IS NULL))")
    ))

(defn build-where-match-statements
  [sql-tablename new-data]
  (clojure.string/join " AND " (map #(update-condition-for-key sql-tablename %) (keys new-data))))

(defn compose-update-sql
  [sql-tablename old-data new-data]
  (let [fields-list (build-fields-list-for-update new-data)
        where-match-statements (build-where-match-statements sql-tablename old-data)]
    [(str
      "UPDATE " sql-tablename "
         SET (" fields-list ") = (SELECT * FROM json_populate_record(null::" sql-tablename ", ?::json))
         FROM (SELECT * FROM json_populate_record(null::" sql-tablename ", ?::json)) __londu_old_data
         WHERE " where-match-statements) (to-json new-data) (to-json old-data)]))

(defn replay-update-in-target [event x-tgt-db]
  (let [schema (:s event)
        table (:t event)
        old-key-values (from-json (:od event))
        new-key-values (from-json (:nd event))
        sql-tablename (quote-schemed-tablename schema table)
        upd-filter (vec (cons (build-where-str old-key-values) (vals old-key-values)))]
    ; (println (str "Updating " old-key-values " to " new-key-values))
    (j/execute! x-tgt-db (compose-update-sql sql-tablename old-key-values new-key-values))
    ))

(defn replay-event-in-target
  "Replays an event in the target database"
  [event x-tgt-db x-src-db]
  (let [op (:op event)]
    (case op
      "INSERT" (replay-insert-in-target event x-tgt-db)
      "DELETE" (replay-delete-in-target event x-tgt-db)
      "UPDATE" (replay-update-in-target event x-tgt-db)
      )
    )
  )