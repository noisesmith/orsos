(ns org.noisesmith.orsos.convert
  (:require [clojure.instant :as instant]
            [clojure.string :as string]))

(defn get-instant
  [ds]
  (let [[m d y] (string/split ds #"/")]
    (instant/read-instant-date (str y \- m \- d))))

(def parsers
  "A map from the keyword in our datomic schema to the function which will
  generate data of that type from the value we will get from the CSV."
  {:string identity
   :instant get-instant
   :enum (comp keyword #(.toLowerCase %))
   :bigdec #(BigDecimal. (or (not-empty %) "0"))
   :boolean {"1" true
             "0" false
             "true" true
             "yes" true
             "false" false
             "no" false}})

(defn get-type
  "Use our datomic schema data structure to find the data converter we need."
  [schema table field]
  (-> schema
      (->> (group-by :name))
      (get (name table))
      (get 0)
      :fields
      (get (name field))
      first
      ;; (doto pprint)
      (parsers identity)))

(defn make-index
  "Generate a lookup table from the fields we want to their position in the
  CSV input, with the help of lookup, which maps from the heading in the CSV
  to the keyword for the field in the schema."
  [lookup fields]
  (reduce (fn [m [n k]]
            (if-let [ky (get lookup k)]
              (assoc m ky n)
              m))
          {}
          (partition
           2
           (interleave (range)
                       fields))))
