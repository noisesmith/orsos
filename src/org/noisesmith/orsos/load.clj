(ns org.noisesmith.orsos.load
  (:refer-clojure :exclude [load])
  (:require [clojure.pprint :as pprint]
            [datomic.api :as datomic]
            [org.noisesmith.ingest :as ingest]
            [org.noisesmith.ingest.convert :as convert]
            [org.noisesmith.orsos.schema :as schema]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(def transaction-dir
  "Directory that holds the cash transaction CSV files."
  "orsos/transactions/")

(def committee-dir
  "Directory that holds the committee data CSV files."
  "orsos/sos_committees")

(defn load-csv
  "Load an individual CSV file from the classpath."
  [path]
  (with-open [source (io/reader (io/resource path))]
    (doall (csv/read-csv source))))

(defn load-csvs
  "Get all the contents from all the cash transaction CSV files in a given
  directory. "
  [from-dir]
  (let [dir (.list (io/file (io/resource from-dir)))]
    (map #(load-csv (str from-dir \/ %)) dir)))


(defn maybe-some
  [n]
  (if n
    #(take n %)
    identity))

(defn run-transaction
  [conn data-list]
  (doseq [data data-list]
    (deref (datomic/transact conn data))))

(defn load-batch
  "Load a batch of csvs"
  [dir field-lookup schema type-lookup & [limit]]
  (let [entries (load-csvs dir)
        lookup-map (ingest/->lookup-map schema field-lookup type-lookup)]
    (for [file ((maybe-some limit) entries)
          :let [ingest-row (ingest/->row lookup-map (first file))]]
      (for [row ((maybe-some limit) (rest file))]
        (ingest-row row)))))

(defn load-all
  "Populate the in-memory datomic database with the downloaded csv data."
  [& [{transaction-directory :transactions
       committee-directory :committees
       :as opts
       :or {transaction-directory transaction-dir
            committee-directory committee-dir}}]]
  (concat (load-batch transaction-directory schema/transaction-lookup
                      schema/orsos-schema convert/get-type)
          (load-batch committee-directory schema/committee-lookup
                      schema/orsos-schema convert/get-type)))
