(ns londu1.operations.json
  (:gen-class)
  (:require [clojure.data.json :as json]))

(defn unjson
  "Extracts json data from the given json payload. Numeric types are handled as bigdecimal for extended accuracy."
  [json-text]
  (json/read-str json-text :bigdec true))