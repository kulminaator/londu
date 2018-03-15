(ns londu1.core
  (:gen-class)
  (require [clojure.java.jdbc :as j])
  )

;; just the example db's being used here
(def pg-source-db {:dbtype "postgresql"
                   :dbname "londu1_test_source_db"
                   :host "127.0.0.1"
                   :user "shopdb_user"
                   :password "shopdb_user"
                   ;; :ssl true
                   ;; :sslfactory "org.postgresql.ssl.NonValidatingFactory"}
                   })

(def pg-target-db {:dbtype "postgresql"
                   :dbname "londu1_test_target_db"
                   :host "127.0.0.1"
                   :user "shopdb_user"
                   :password "shopdb_user"
                   ;; :ssl true
                   ;; :sslfactory "org.postgresql.ssl.NonValidatingFactory"}
                   })

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
  )
