(ns org.noisesmith.orsos
  (:require [clojure.pprint :as pprint]
            [datomic.api :as datomic]
            [org.noisesmith.orsos.datomic :as debug]
            [org.noisesmith.orsos.load :as load])
  (:gen-class))

(def db-uri "datomic:mem://orsos")

(defonce created
  (delay (datomic/create-database db-uri)))

(defonce conn
  (delay
   @created
   (datomic/connect db-uri)))

(defonce debug (atom nil))

(defn -main
  [& args]
  (load/setup-schema @conn)
  (let [source-data (load/load-all conn {:limit 1})]
    ;; (debug/values (datomic/db @conn))
    (reset! debug source-data)
    (load/run-transaction @conn source-data)
    #_
    (pprint/pprint
     (datomic/q '[:find (pull ?e [*])
                  :where [?e :committee/committee-name "Kevin F Neely"]]
                (datomic/db @conn)))))
