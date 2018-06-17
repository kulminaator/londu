(ns londu1.logger
  (:gen-class)
  (:require [clojure.data.json :as json]))

(defn timestamp []
 (str (java.time.LocalDateTime/now)))

; {"timestamp" (timestamp) "level" "pat" "message" "wat" "extra" nil}
(defn reformat [level txt extra]
  (str (json/write-str {"timestamp" (timestamp) "level" (name level) "message" txt "extra" extra}) "\n"))

(defn current-logfile []
  (str "logs/" (java.time.LocalDate/now) "-londu.log"))

(defn to-file [level txt extra]
  (spit (current-logfile) (reformat level txt extra) :append true))

;; (debug "hello")
(defn debug [txt  & extra]
  (to-file :debug txt extra))

(defn info [txt  & extra]
  (to-file :debug txt extra))

(defn error [txt  & extra]
  (to-file :debug txt extra))