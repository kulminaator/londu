(ns londu1.operations.event-replay
  (:gen-class)
  (:use [londu1.operations.json :only [from-json]])
  (:require [clojure.java.jdbc :as j]))

(defn replay-insert-in-target [event x-tgt-db]
  (let [schema (:s event)
        table (:t event)
        key-values (from-json (:nd event))]
    ; (println (str "Inserting " key-values))
    (j/insert! x-tgt-db (str schema "." table) key-values)
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

(defn replay-update-in-target [event x-tgt-db]
  (let [schema (:s event)
        table (:t event)
        old-key-values (from-json (:od event))
        new-key-values (from-json (:nd event))
        upd-filter (vec (cons (build-where-str old-key-values) (vals old-key-values)))]
    ; (println (str "Updating " old-key-values " to " new-key-values))
    (j/update! x-tgt-db (str schema "." table) new-key-values upd-filter)
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