(ns org.noisesmith.orsos.load
  (:refer-clojure :exclude [load])
  (:require [clojure.pprint :as pprint]
            [datomic.api :as datomic]
            [org.noisesmith.orsos.convert :as convert]
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
  [path]
  "Load an individual CSV file from the classpath."
  (with-open [source (io/reader (io/resource path))]
    (doall (csv/read-csv source))))

(defn load-csvs
  "Get all the contents from all the cash transaction CSV files in a given
  directory. "
  [from-dir]
  (let [dir (.list (io/file (io/resource from-dir)))]
    (map #(load-csv (str from-dir \/ %)) dir)))


(defn build-transaction
  "Generate the :db/add clauses to insert one row from a CSV."
  [index-lookup refs schema]
  (fn builder [tran]
    ;; entities are all the entities to be created from this line of the csv
    ;; see the lookup table in schema.clj - each field is mapped to N entities
    (let [entity-pairs (map (fn [[k v]]
                              [v (datomic/tempid :db.part/user)])
                            refs)
          entities (into {} entity-pairs)
          entries
          (reduce (fn [acc [k [idx eid]]]
                    (let [id (get entities eid)
                          typer (convert/get-type schema k)
                          val (get tran idx)
                          value (and (not-empty val)
                                     (try
                                       (typer val)
                                       (catch Exception e
                                         (println e "Load: bad val:" val))))]
                      (if value
                        (conj acc [:db/add id k value])
                        acc)))
                  []
                  index-lookup)
          ;; those entities that are referenced
          referenced (into #{} (map second entries))
          ;; reverse lookup of entity to index
          r-entities (into {} (for [[k v] entity-pairs] [v k]))
          ;; id / index pairs for referenced entities
          ref (select-keys r-entities referenced)
          ;; reverse lookup of index  to datomic key
          r-refs (into {} (for [[k v] refs :when (keyword? k)] [v k]))
          ;; add statements to associate refs
          main-id (get entities 0)
          more-entries
          (reduce (fn [acc [datomic-id idx]]
                    (if-let [datom-key (get r-refs idx)]
                      (conj acc [:db/add main-id datom-key datomic-id])
                      acc))
                  []
                  ref)]
      ;; (pprint/pprint {:ref ref :more-entries more-entries})
      (into entries more-entries))))

(defn build-transactions
  "Generate input suitable for the datomic database from a CSV file."
  [trans schema lookup]
  (let [fields (first trans)
        index-lookup (convert/make-index lookup fields)]
    (mapcat (build-transaction index-lookup (:ref lookup) schema)
            (->> trans
                 rest
                 ;; (take 2)
                 ))))

(defn transaction-runner
  "Construct an executor for transactions with this connection."
  [conn schema lookup]
  (fn run-transaction
    ;; Run a transaction for the the data supplied.
    [data]
    (let [runnable (build-transactions data schema lookup)]
      ;; (pprint/pprint {:runnable runnable})
      (deref (datomic/transact conn runnable)))))

(defn load-all
  "Populate the in-memory datomic database with the downloaded csv data."
  [conn & [{transaction-directory :transactions
             committee-directory :committees
             :as opts
             :or {transaction-directory transaction-dir
                  committee-directory committee-dir}}]]
  (let [orso-transactions (load-csvs transaction-directory)
        lookup schema/transaction-lookup
        schema schema/orsos-schema]
    (doseq [transaction (->> orso-transactions
                             ;; (take 2)
                             )]
      ((transaction-runner conn schema lookup) transaction)))
  (let [orsos-committees (load-csvs committee-directory)
        lookup schema/committee-lookup
        schema schema/orsos-schema]
    (doseq [transaction (->> orsos-committees
                             ;; (take 2)
                             )]
      ((transaction-runner conn schema lookup) transaction))))
