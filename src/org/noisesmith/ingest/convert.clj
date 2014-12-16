(ns org.noisesmith.ingest.convert
  (:require [clojure.instant :as instant]
            [clojure.string :as string]))

(defn get-instant
  [ds]
  (let [[m d y] (string/split ds #"/")]
      (instant/read-instant-date (str y \- m \- d))))

(defn parse-num
  [^String s]
  (try
    (-> s
        (.replaceAll "(?i)zero" "0")
        (.replaceAll "(?i)none" "0")
        (.replaceAll "(?i)o" "0")
        (->> (re-find #"[0-9\.]+"))
        (#(BigDecimal. ^String %)))
    (catch Exception _
      (println "bad num" s)
      0M)))

(defn parsers
  "A map from the keyword in our datomic schema to the function which will
  generate data of that type from the value we will get from the CSV."
  [[from spec space field]]
  ;; (println "parse" (pr-str from) (pr-str spec))
  (get 
   {:string identity
    :instant get-instant
    :enum #(keyword (str space \. field) (.toLowerCase ^String %))
    :bigdec parse-num
    :boolean {"1" true
              "0" false
              "true" true
              "yes" true
              "false" false
              "no" false}}
   from
   identity))

(defn get-type
  "Use our datomic schema data structure to find the data converter we need."
  [schema field]
  (-> schema
      (->> (filter #(= (:name %) (namespace field))))
      first
      :fields
      (get (name field))
      (conj (namespace field))
      (conj (name field))
      parsers))
