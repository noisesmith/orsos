(ns org.noisesmith.orsos
  (:require [clojure.pprint :as pprint]
            [datomic.api :as datomic]
            [org.noisesmith.orsos.datomic :as debug]
            [org.noisesmith.orsos.schema :as schema]
            [org.noisesmith.orsos.load :as load])
  (:gen-class))

(def db-uri "datomic:mem://orsos")

(defonce created
  (delay (datomic/create-database db-uri)))

(defonce conn
  (delay
   @created
   (datomic/connect db-uri)))


(defn -main
  [& args]
  (let [schema (doto (schema/get-schema)
                 (#(deref (datomic/transact @conn %))))
        source-data (load/load-all)]
    ;; (debug/values (datomic/db @conn))
    (doseq [source source-data]
      (load/run-transaction @conn source))
    (pprint/pprint
     (datomic/q '[:find (pull ?e [*])
                  :where [?e :committee/committee-name]]
                (datomic/db @conn)))))
