(ns org.noisesmith.orsos.datomic
  (:require [datomic.api :as datomic]))

(defn values
  [^datomic.db.Db db]
  (let [it ^datomic.iter.Iterator (.iterator (datomic/datoms db :eavt))]
    ((fn all []
       (lazy-seq
        (when (.hasNext it)
          (cons (.next it)
                (all))))))))
