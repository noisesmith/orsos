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
  (deref (datomic/transact conn data-list)))

(defn upsert-unique-to
  "Take the normalized entities (such that each unique item is only listed once)
  and find their upsert, if any."
  [conn]
  (fn upsert-unique
    [{k :key v :value id :id :as entry}]
    (let [duplicate (not-empty
                     (and (not (coll? v))
                          (datomic/q [:find '?e :where ['?e k v]]
                                     (datomic/db conn))))]
      (when duplicate
        {:k k :v v :id id :duplicate-of (first duplicate)}))))

(defn combine-unique
  "Combine entries that have the same key and value."
  [entries conn]
  ;; TODO: we need to inform the caller of duplication, so that insertions
  ;; with refs to the item get the right tid
  (let [grouped (group-by (juxt :key :value) entries)
        merged-up (into {} (map (fn [[k v]]
                                  [(:id (first v))
                                   {:key k
                                    :value v
                                    :vs (map :id (rest v))}])
                                grouped))
        upserted (keep (comp (upsert-unique-to conn)
                             first
                             val)
                       merged-up)
        upserts (group-by :id upserted)]
    [merged-up upserts]))

(defn normalize-entities
  "Take the raw entries derived from the rows of the CSV, and normalize to the
  datomic db entities that will be created, such that any references to the same
  db entity are grouped."
  [entries conn]
  (let [{unique true
         others false
         :as split} (group-by (comp boolean :unique)
                              entries)
        [merged-up upserts] (combine-unique unique conn)
        [merged
         replacements] (reduce (fn [[merged replacements] [orig-id merges]]
                                 (let [id (get upserts orig-id orig-id)]
                                   [(conj merged
                                      {:id id
                                       :key (:key merges)
                                       :value (:value merges)})
                                    (reduce (fn [replacements merge]
                                              (assoc replacements merge id))
                                            replacements
                                            (:vs merges))]))
                               [[] {}]
                               merged-up)]
    (map (fn [{id :id v :value key :key :as entry}]
           (let [normalized-id (get replacements id id)
                 normalized-val (get replacements v v)]
             [:db/add
              :id normalized-id
              :key key
              :value normalized-val]))
         (concat others merged))))

(defn pre-process
  [insertions conn]
  (normalize-entities insertions conn))

(defn load-batch
  "Load a batch of csvs"
  [dir field-lookup schema type-lookup limit conn]
  (let [entries (load-csvs dir)
        lookup-map (ingest/->lookup-map schema field-lookup type-lookup)]
    (pre-process
     (apply
      concat
      (for [file ((maybe-some limit) entries)
            :let [ingest-row (ingest/->row lookup-map (first file))]]
        (for [row ((maybe-some limit)
                   (rest file))
              ingested  (ingest-row row)]
          ingested)))
     conn)))

(defn load-all
  "Populate the in-memory datomic database with the downloaded csv data."
  [conn & [{transaction-directory :transactions
            committee-directory :committees
            limit :limit
            :as opts
            :or {transaction-directory transaction-dir
                 committee-directory committee-dir
                 limit nil}}]]
  (concat (load-batch transaction-directory
                      schema/transaction-lookup
                      schema/orsos-schema
                      convert/get-schema-info
                      limit
                      @conn)
          (load-batch committee-directory
                      schema/committee-lookup
                      schema/orsos-schema
                      convert/get-schema-info
                      limit
                      @conn)))
(defn setup-schema
  [conn]
  (let [computed-schema (schema/get-schema)
        schema @(datomic/transact conn computed-schema)]
    schema))
