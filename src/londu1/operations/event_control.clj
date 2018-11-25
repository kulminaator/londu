(ns londu1.operations.event-control
  (:gen-class)
  (:use [londu1.operations.json :only [unjson]])
  (:require [clojure.java.jdbc :as j]))


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