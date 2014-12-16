(ns org.noisesmith.ingest
  (:require [clojure.pprint :as pprint]
            [datomic.api :as datomic]))

(defn ->lookup-map
  "Construct a map from heading in the CSV file to a data structure describing
  how to intern a line in that CSV into the db."
  [schema lookup type-lookup]
  (reduce (fn [m heading]
            (if-let [mappings
                     (not-empty
                      (for [[eid ky] (get lookup heading)
                            :let [typer (type-lookup schema ky)]]
                        [ky {:entity-id eid
                             :conversion typer}]))]
              (assoc m heading mappings)
              m))
          {:ref (:ref lookup)}
          (keys (dissoc lookup :ref))))

(defn convert
  "Convert a value from potentially dodgy runtime data."
  [converter value]
  (and converter
       (not-empty value)
       (try
         (converter value)
         (catch Exception e
           (println e "ingest/convert: bad value:" converter value)))))

(defn ->row
  "Returns the function to load a row, given a lookup-map and a sequence of
  headings."
  [lookup-map headings]
  (let [get-entities
        (fn []
          (reduce (fn [m [entity-key entity-index]]
                    (assoc m
                      entity-index
                      [entity-key
                       (delay
                        (datomic/tempid :db.part/user))]))
                  {}
                  (:ref lookup-map)))
        lookups (map lookup-map headings)]
    (fn get-row [row]
      ;; this is the function to load one row, given the headings and the lookup
      (let [entities (get-entities)
            key-insertions
            ;; doall is needed so that ref-insertions can work
            (doall (for [[entries value] (partition 2 (interleave lookups row))
                         [entity-key entry] entries
                         :let [typer (:conversion entry)
                               v (convert typer value)
                               [_ id-ref] (entities (:entity-id entry))]
                         :when v]
                     [:db/add @id-ref entity-key v]))
            [_ main-id-ref] (get entities 0)
            main-id @main-id-ref
            ref-insertions (for [[key id-ref] (vals (dissoc entities 0))
                                 :when (realized? id-ref)]
                             [:db/add main-id key @id-ref])]
        ;; (pprint/pprint {:ref-insertions ref-insertions})
         (into key-insertions ref-insertions)))))
