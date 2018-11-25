(ns londu1.core
  (:gen-class)
  (:require [clojure.java.jdbc :as j])
  (:require [clojure.data.json :as json]))

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

(defn unjson
  [x]
  (json/read-str x :bigdec true))

(defn create-tick [db]
  (j/execute! db "INSERT INTO __londu_1.ticks(created_at) VALUES(default)"))

(defn replay-insert-in-target [event x-tgt-db]
  (let [schema (:s event)
        table (:t event)
        key-values (unjson (:nd event))]
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
        old-key-values (unjson (:od event))
        del-values (vec (cons (build-where-str old-key-values) (vals old-key-values)))]
    ; (println (str "Deleting " old-key-values))
    (j/delete! x-tgt-db (str schema "." table) del-values)
    ))

(defn replay-update-in-target [event x-tgt-db]
  (let [schema (:s event)
        table (:t event)
        old-key-values (unjson (:od event))
        new-key-values (unjson (:nd event))
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

(defn get-unreplicated-events
  "Returns the list of unreplicated data events from src-db"
  [x-src-db previous]
  (if (nil? previous)
    (j/query x-src-db ["SELECT * FROM __londu_1.events ORDER BY id"])
    (j/query x-src-db ["SELECT * FROM __londu_1.events WHERE id > ? ORDER BY id" (:id previous)])))

(defn find-last-event
  "Returns the last replicated event in target. So we know from where to continue."
  [tgt-db]
  (first (j/query tgt-db
           ["SELECT * FROM __londu_1.events WHERE id = (SELECT event_id FROM __londu_1.states WHERE id=1)"])))

(defn record-last-event-in-target
 "Writes down the id of the last event into the target database"
 [event x-tgt-db]
 ;;(println "Recording the last state")
 (when (= (j/update! x-tgt-db "__londu_1.states" {"event_id" (:id event)} ["id = ?" 1]) '(0))
   (j/insert! x-tgt-db "__londu_1.states" {"id" 1 "event_id" (:id event)})))

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

(defn compose-create-trigger [qtablename]
  ;; todo aside from normalizing the table name should also validate that it can be a table name at all.
  ;; it may carry special symbols so will double quote it anyway here
  (let [safe-tablename (clojure.string/replace qtablename #"[^a-zA-Z0-9_]" "_")
        [schema tablename] (clojure.string/split qtablename #"\.")]
    (str "CREATE TRIGGER __londu_1_trigger_" safe-tablename
      " BEFORE INSERT OR UPDATE OR DELETE ON \"" schema "\".\"" tablename "\""
        " FOR EACH ROW EXECUTE PROCEDURE __londu_1.trigger();")
    )
  )


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
  (let [[schema table] (clojure.string/split qtablename #"\.")]
    (j/execute! x-src-db(compose-declare-read-cursor (str schema "." table)))
    (copy-table-data x-src-db x-tgt-db (str schema "." table))
    (j/execute! x-src-db(compose-close-read-cursor (str schema "." table)))
    ))

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
                                                (j/execute! x-src-trigger-con (compose-create-trigger tablename))
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