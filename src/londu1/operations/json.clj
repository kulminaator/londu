(ns londu1.operations.json
  (:gen-class)
  (:require [clojure.data.json :as json]))

(defn from-json
  "Extracts json data from the given json payload. Numeric types are handled as bigdecimal for extended accuracy."
  [json-text]
  (json/read-str json-text :bigdec true))

(defn to-json
  "Transforms a clojure value into json. Numeric types are handled as bigdecimal for extended accuracy."
  [clojure-data]
  (json/write-str clojure-data :bigdec true))