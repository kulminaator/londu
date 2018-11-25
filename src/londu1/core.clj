(ns londu1.core
  (:gen-class)
  (:require [clojure.java.jdbc :as j])
  (:use [londu1.operations.table :only [table-copy create-trigger]])
  (:use [londu1.operations.event-control :only [find-last-event record-last-event-in-target get-unreplicated-events]])
  (:use [londu1.operations.event-replay :only [replay-event-in-target]]))

;; just the example db's being used here
(def pg-source-db {:dbtype "postgresql"
                   :dbname "londu1_test_source_db"
                   :host "127.0.0.1"
                   :port 5432
                   :user "shopdb_source_user"
                   :password "shopdb_source_user"
                   ;; :ssl true
                   ;; :sslfactory "org.postgresql.ssl.NonValidatingFactory"}
                   })

(def pg-target-db {:dbtype "postgresql"
                   :dbname "londu1_test_target_db"
                   :host "127.0.0.1"
                   :port 5432
                   :user "shopdb_target_user"
                   :password "shopdb_target_user"
                   ;; :ssl true
                   ;; :sslfactory "org.postgresql.ssl.NonValidatingFactory"}
                   })

(defn create-tick [db]
  (j/execute! db "INSERT INTO __londu_1.ticks(created_at) VALUES(default)"))

(defn replicate-step-in-tx
  "Does the replicatin step from in-transaction connecton src to in-transactin connection tgt"
  [x-src-db x-tgt-db previous]
  (let [events (get-unreplicated-events x-src-db previous)]
    (doseq [ev events]
      (println ev)
      ; (println (:nd ev))
      (replay-event-in-target ev x-tgt-db x-src-db))
    (when-not (empty? events)
      (record-last-event-in-target (last events) x-tgt-db))
    (last events)))

(defn replicate-step
  "Does the replication of data if available in the events table"
  [src-db tgt-db previous]
  (let [last_replicated_event
        (j/with-db-transaction [source-con src-db]
                               (j/with-db-transaction [target-con tgt-db]
                                                      (replicate-step-in-tx source-con target-con previous))
                         )]
    (println (str "-- Last: " last_replicated_event))
    (if (nil? last_replicated_event)
      previous
      last_replicated_event)
    )
  )

(defn replicate-batch-of-steps
  "Invokes the single step replicator for a set of times"
  [src-db tgt-db]
  (loop [counter 60
         last (find-last-event tgt-db)]
    (Thread/sleep 1000)
    (when (> counter 0) (recur (dec counter) (replicate-step src-db tgt-db last)))))


(defn sync-transactions [tx-a tx-b]
  (println "syncing snapshots")
  (-> (j/query tx-a "SELECT pg_export_snapshot() snap")
      (first)
      (:snap)
      ((fn [snap]
         (println (str "Snapshot is " snap))
         (j/execute! tx-b (str "SET TRANSACTION SNAPSHOT '" snap "'") ))))

  )

(defn add-table-to-replication [src-db tgt-db tablename]
  (j/with-db-transaction [x-src-copy-con src-db {:isolation :repeatable-read}]
                         (j/with-db-transaction [x-src-trigger-con src-db {:isolation :repeatable-read}]
                                                (println "creating trigger")
                                                (create-trigger x-src-trigger-con tablename)
                                                ;; creation of trigger above nicely locked down our tx to a sweetspot
                                                (sync-transactions x-src-trigger-con x-src-copy-con))
                         (println "performing copy")
                         (j/with-db-transaction [x-tgt-con tgt-db]
                                                (table-copy tablename x-src-copy-con x-tgt-con)
                                                )
                         )
  )

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
  ;(replicate-step pg-source-db pg-target-db nil)
  ;(replicate-batch-of-steps pg-source-db pg-target-db)

  )

;; (add-table-to-replication pg-source-db pg-target-db "public.shop_items")

;; (add-table-to-replication pg-source-db pg-target-db "add_test_subject")