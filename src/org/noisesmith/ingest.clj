(ns org.noisesmith.ingest
  (:require [clojure.pprint :as pprint]
            [datomic.api :as datomic]))

(defn ->lookup-map
  "Construct a map from heading in the CSV file to a data structure describing
  how to intern a line in that CSV into the db."
  [schema lookup schema-info]
  (reduce (fn [m heading]
            (if-let [mappings
                     (not-empty
                      (for [[eid ky] (get lookup heading)
                            :let [[typer
                                   [type-key
                                    flags
                                    key-space
                                    key-name]] (schema-info schema ky)
                                  unique (or (:unique-value flags)
                                             (:unique-identity flags))]]
                        [ky {:entity-id eid
                             :unique unique
                             :flags flags
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

(def debug (promise))

(defn ->row
  "Returns the function to load a row, given a lookup-map and a sequence of
  headings."
  [lookup-map headings]
  (let [get-entities
        (fn []
          (reduce (fn [m [entity-key entity-index]]
                    ;; for each key in ref, create an id, mapped from the index
                    (assoc m
                      entity-index
                      [entity-key (delay (datomic/tempid :db.part/user))]))
                  {}
                  (:ref lookup-map)))
        lookups (map lookup-map headings)]
    (fn get-row [row]
      ;; this is the function to load one row, given the headings and the lookup
      ;; returns a simple data structure that is executable by the loader
      (let [entities (get-entities)
            entries-and-values (partition 2 (interleave lookups row))
            key-insertions
            ;; doall is needed so that ref-insertions can work
            (doall
             (for [[entries value] entries-and-values
                   [entity-key
                    {typer :conversion
                     entity-id :entity-id
                     flags :flags
                     unique :unique
                     :as entry}] entries
                   :let [v (convert typer value)
                         [_ id] (entities entity-id)]
                   :when v]
               {:entity-id entity-id
                :key entity-key
                :value v
                ;; :insert [:db/add  @id entity-key v]
                :id @id
                :unique unique
                :flags flags}))
            [_ main-id] (get entities 0)
            ref-insertions (for [[key id] (vals (dissoc entities 0))
                                 :when (realized? id)]
                             ;; mark this as a ref, so that its value can
                             ;; be normalized if it is merged
                             {:ref true
                              :key key
                              :value @id
                              ;; :insert [:db/add @main-id key @id]
                              :id @main-id
                              :flags #{}})]
         (into key-insertions ref-insertions)))))
