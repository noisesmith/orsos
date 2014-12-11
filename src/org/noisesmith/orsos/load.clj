(ns org.noisesmith.orsos.load
  (:refer-clojure :exclude [load])
  (:require [datomic.api :as datomic]
            [org.noisesmith.orsos.convert :as convert]
            [org.noisesmith.orsos.schema :as schema]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(def transaction-dir
  "orsos/transactions/")

(defn load-csv
  [path]
  (with-open [source (io/reader (io/resource path))]
    (doall (csv/read-csv source))))

(def load-transactions
  (memoize
   (fn []
     (let [dir (.list (io/file (io/resource transaction-dir)))]
       (map #(load-csv (str transaction-dir \/ %)) dir)))))


(defn build-transaction
  [index-lookup]
  (fn builder [tran]
    [(reduce (fn [v [k idx]]
               (let [typer (convert/get-type schema/orsos-schema
                                             "transaction" k)
                     value (get tran idx)]
                 (conj v k (typer value))))
             [:db/add (datomic/tempid :db.part/user)]
             index-lookup)]))

(defn build-transactions
  [conn]
  (fn [trans]
    (let [fields (first trans)
          lookup schema/transaction-lookup
          index-lookup (convert/make-index lookup fields)]
      (mapv 
       #(deref
         (datomic/transact
          conn
          ((build-transaction index-lookup) %)))
       (take 2 (rest trans))))))

(defn load-all
  [conn]
  "populate the in-memory datomic database with the downloaded csv data"
  (let [orso-transactions (load-transactions)]
    (mapv (build-transactions conn)
          (take 2 orso-transactions))))
