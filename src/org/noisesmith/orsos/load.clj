(ns org.noisesmith.orsos.load
  (:refer-clojure :exclude [load])
  (:require [datomic.api :as datomic]
            [org.noisesmith.orsos.convert :as convert]
            [org.noisesmith.orsos.schema :as schema]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(def transaction-dir
  "Directory that holds the cash transaction CSV files."
  "orsos/transactions/")

(defn load-csv
  [path]
  "Load an individual CSV file from the classpath."
  (with-open [source (io/reader (io/resource path))]
    (doall (csv/read-csv source))))

(def load-transactions
  "Get all the contents from all the cash transaction CSV files in a given
  directory.
  Memoized for development purposes."
  (memoize
   (fn [from-dir]
     (let [dir (.list (io/file (io/resource from-dir)))]
       (map #(load-csv (str from-dir \/ %)) dir)))))


(defn build-transaction
  "Generate the :db/add clauses to insert one row from a CSV."
  [index-lookup]
  (fn builder [tran]
    (let [id (datomic/tempid :db.part/user)] ;; every insert is to the same id
      (reduce (fn [v [k idx]]
                (let [typer (convert/get-type schema/orsos-schema
                                              "transaction" k)
                      val (get tran idx)
                      value (and (not-empty val)
                                 (try
                                   (typer val)
                                   (catch Exception e
                                     (println e "Load: bad val:" val))))]
                  (if value
                    (conj v [:db/add id k value])
                    v)))
              []
              index-lookup))))

(defn build-transactions
  "Generate input suitable for the datomic database from a CSV file."
  [trans]
  (let [fields (first trans)
        lookup schema/transaction-lookup
        index-lookup (convert/make-index lookup fields)]
    (mapcat (build-transaction index-lookup)
            (->> trans
                 rest
                 (take 2)))))

(defn transaction-runner
  "Construct an executor for transactions with this connection."
  [conn]
  (fn run-transaction
    ;; Run a transaction for the the data supplied.
    [data]
    (deref (datomic/transact conn (build-transactions data)))))

(defn load-all
  "Populate the in-memory datomic database with the downloaded csv data."
  [conn & {transaction-directory :transactions
           :as opts
           :or {transaction-directory transaction-dir}}]
  (let [orso-transactions (load-transactions transaction-dir)]
    (mapv (transaction-runner conn)
          (->> orso-transactions
               (take 2)))))
