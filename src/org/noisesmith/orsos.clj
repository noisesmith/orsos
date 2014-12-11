(ns org.noisesmith.orsos
  (:require [datomic.api :as datomic]
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
  (let [schema (schema/get-schema)]
    @(datomic/transact @conn schema)
    (load/load-all @conn)
    (datomic/q '[:find [?e ...] :where [?e :transaction/filer]]
               (datomic/db @conn))))
