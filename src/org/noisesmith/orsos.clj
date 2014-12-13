(ns org.noisesmith.orsos
  (:require [clojure.pprint :as pprint]
            [datomic.api :as datomic]
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
    (pprint/pprint
     (datomic/q '[:find (pull ?e [*]) :where [?e :committee/committee-name]]
                (datomic/db @conn)))))
