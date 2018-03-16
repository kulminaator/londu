(ns londu1.core
  (:gen-class)
  (:require [clojure.java.jdbc :as j])
  (:require [clojure.data.json :as json])
  )

;; just the example db's being used here
(def pg-source-db {:dbtype "postgresql"
                   :dbname "londu1_test_source_db"
                   :host "127.0.0.1"
                   :port 5435
                   :user "shopdb_user"
                   :password "shopdb_user"
                   ;; :ssl true
                   ;; :sslfactory "org.postgresql.ssl.NonValidatingFactory"}
                   })

(def pg-target-db {:dbtype "postgresql"
                   :dbname "londu1_test_target_db"
                   :host "127.0.0.1"
                   :port 5435
                   :user "shopdb_user"
                   :password "shopdb_user"
                   ;; :ssl true
                   ;; :sslfactory "org.postgresql.ssl.NonValidatingFactory"}
                   })

(def replicated_tables
  '("shop_items" "shop_workers"))


(defn unjson
  [x]
  (json/read-str x :bigdec true))

(defn replay-insert-in-target [event tgt-db]
  (let [schema (:s event)
        table (:t event)
        key-values (unjson (:nd event))]
    ; (println (str "Inserting " key-values))
    (j/insert! tgt-db (str schema "." table) key-values)
    ))

(defn build-where-str [key-values]
  (let [ks (keys key-values)]
    (clojure.string/join " AND " (map #(str "\"" % "\" = ?") ks))
    ))

(defn replay-delete-in-target [event tgt-db]
  (let [schema (:s event)
        table (:t event)
        old-key-values (unjson (:od event))
        del-values (vec (cons (build-where-str old-key-values) (vals old-key-values)))]
    ; (println (str "Deleting " old-key-values))
    (j/delete! tgt-db (str schema "." table) del-values)
    ))

(defn replay-update-in-target [event tgt-db]
  (let [schema (:s event)
        table (:t event)
        old-key-values (unjson (:od event))
        new-key-values (unjson (:nd event))
        upd-filter (vec (cons (build-where-str old-key-values) (vals old-key-values)))]
    ; (println (str "Updating " old-key-values " to " new-key-values))
    (j/update! tgt-db (str schema "." table) new-key-values upd-filter)
    ))

(defn replay-event-in-target
  "Replays an event in the target database"
  [event tgt-db]
  (let [op (:op event)]
    (case op
      "INSERT" (replay-insert-in-target event tgt-db)
      "DELETE" (replay-delete-in-target event tgt-db)
      "UPDATE" (replay-update-in-target event tgt-db))
    )
  )

(defn get-unreplicated-events
  "Returns the list of unreplicated data events from src-db"
  [src-db previous]
  (if (nil? previous)
    (j/query pg-source-db ["SELECT * FROM __londu_1_events ORDER BY tid, id"])
    (j/query pg-source-db ["SELECT * FROM __londu_1_events WHERE id > ? ORDER BY tid, id" (:id previous)])))

(defn replicate-step-in-tx
  "Does the replicatin step from in-transaction connecton src to in-transactin connection tgt"
  [src tgt previous]
  (let [events (get-unreplicated-events src previous)]
    (doseq [ev events]
      (println ev)
      ; (println (:nd ev))
      (replay-event-in-target ev tgt))
    (last events)))

(defn replicate-step
  "Does the replication of data if available in the events table"
  [src tgt previous]
  (let [last_replicated_event
        (j/with-db-transaction [source-con src]
                               (j/with-db-transaction [target-con tgt]
                                                      (replicate-step-in-tx source-con target-con previous))
                         )]
    (println (str "-- Last: " last_replicated_event))
    last_replicated_event
    )
  )

(defn replicate-batch-of-steps
  "Invokes the single step replicator for a set of times"
  [src tgt]
  (doseq [_ (range 1 1000)]
    ; (replicate-step src tgt last)
    (Thread/sleep 10)))

(defn source-db-connect-test []
  (println (j/query pg-source-db
           ["select now();"])))

(defn target-db-connect-test []
  (println(j/query pg-target-db
           ["select now();"])))

(defn -main
  "The starter of the application."
  [& args]
  (println "Hello dark world!")
  (source-db-connect-test)
  (target-db-connect-test)
  (println "Got the db connections!")
  ;(replicate-step pg-source-db pg-target-db)
  ;(replicate-batch-of-steps pg-source-db pg-target-db)

  )
