(ns org.noisesmith.orsos.datomic
  (:require [datomic.api :as datomic]))

(defn values
  [db]
  (let [it (.iterator (datomic/datoms db :eavt))]
    ((fn all []
       (lazy-seq
        (when (.hasNext it)
          (cons (.next it)
                (all))))))))
