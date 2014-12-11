(ns org.noisesmith.orsos.convert
  (:require [clojure.instant :as instant]
            [clojure.string :as string]))

(defn get-instant
  [ds]
  (let [[m d y] (string/split ds #"/")]
    (instant/read-instant-date (str y \- m \- d))))

(def parsers
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
