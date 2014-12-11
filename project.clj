(defproject orsos "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0" #_"1.7.0-alpha4"]
                 [com.datomic/datomic-free "0.9.5078"]
                 [datomic-schema "1.1.0"]
                 [org.clojure/data.csv "0.1.2"]]
  :main ^:skip-aot org.noisesmith.orsos
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
