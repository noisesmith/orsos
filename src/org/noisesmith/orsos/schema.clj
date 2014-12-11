(ns org.noisesmith.orsos.schema
  (:require [datomic.api :as datomic]
            [datomic-schema.schema :as d-schema]))

(def orsos-schema
  [(d-schema/schema orsos
     (d-schema/fields
       [source-id :long :indexed
        "Source ID from Oregon Secretary of State website"]))
   (d-schema/schema entity
     (d-schema/fields
       [name        :string :indexed]
       [entity-type :string :indexed]
       [employer    :ref]))
   (d-schema/schema person
     (d-schema/fields
       [first-name       :string]
       [last-name        :string]
       [residence-phone  :string]
       [work-phone       :string]
       [fax-phone        :string]
       [email            :string]
       [title            :string]
       [occupation       :string]
       [is-employed      :boolean]
       [is-self-employed :boolean]))
   (d-schema/schema address
     (d-schema/fields
       [full          :string]
       [street        :string]
       [street-line2  :string]
       [city          :string]
       [state         :string]
       [zip           :string]
       [zip-plus-four :string]
       [state         :string]))
   (d-schema/schema measure
     (d-schema/fields
       [name :string  :indexed]
       [year :long    :indexed]))
   (d-schema/schema position
     (d-schema/fields
       [name  :string :indexed "candidate office name"]
       [group :string :indexed "candidate office group"]))
   (d-schema/schema committee
     (d-schema/fields
       [committee-type            :enum [:cpc :cc :pac]]
       [committee-subtype         :string]
       [filing-date               :instant]
       [organization-filing-date  :instant]
       [treasurer                 :ref]
       [treasurer-mailing-address :ref]
       [candidate                 :ref]
       [candidate-mailing-address :ref]
       [active-election           :string]
       [supports-measures         :ref :many]
       [opposes-measures          :ref :many]))
   (d-schema/schema trans-subtype
     (d-schema/fields
       [name      :string :indexed]
       [direction :enum [:in :out]]
       [type      :string :indexed]))
   (d-schema/schema transaction
     (d-schema/fields
       [original-id               :string]
       [transaction-date          :instant :indexed]
       [status                    :enum [:original :amended]]
       [transaction-subtype       :ref :indexed]
       [amount                    :bigdec]
       [aggregate-amount          :bigdec]
       [filer
        :ref "Reference to committee that filed this transaction"]
       [contributor-payee
        :ref "Contributor Payee - maybe a committee too"]
       [attest-by                 :ref]
       [attest-date               :instant]
       [review-by                 :ref]
       [review-date               :instant]
       [filed-by                  :ref]
       [filed-date                :instant]
       [due-date                  :instant]
       [payment-schedule          :string]
       [purpose-description       :string]
       [interest-rate             :bigdec]
       [check-number              :string]
       [occupation-letter-date    :instant]
       [contributor-payee-agent   :ref]
       [contributor-payee-address :ref]
       [contributor-payee-county  :string]
       [purpose-codes             :string]
       [expired-date              :instant]
       [is-trans-stsfd            :boolean]))])

(def transaction-lookup
  {"Tran Id" nil
   "Original Id" :transaction/original-id
   "Tran Date" :transaction/transaction-date
   "Tran Status" :transaction/status
   "Filer" :transaction/filer
   "Contributor/Payee" :transaction/contributor-payee
   "Sub Type" :transaction/transaction-subtype
   "Amount" :transaction/amount
   "Aggregate Amount" :transaction/aggregate-amount
   "Contributor/Payee Committee ID" :transaction/contributor-payee
   "Filer Id" nil
   "Attest By Name" nil
   "Attest Date" nil
   "Review By Name" nil
   "Review Date" nil
   "Due Date" nil
   "Occptn Ltr Date" nil
   "Pymt Sched Txt" nil
   "Purp Desc" nil
   "Intrst Rate" nil
   "Check Nbr" nil
   "Tran Stsfd Ind" nil
   "Filed By Name" nil
   "Filed Date" nil
   "Addr book Agent Name" nil
   "Book Type" nil
   "Title Txt" nil
   "Occptn Txt" nil
   "Emp Name" nil
   "Emp City" nil
   "Emp State" nil
   "Employ Ind" nil
   "Self Employ Ind" nil
   "Addr Line1" nil
   "Addr Line2" nil
   "City" nil
   "State" nil
   "Zip" nil
   "Zip Plus Four" nil
   "County" nil
   "Purpose Codes" nil
   "Exp Date" nil})

(defn get-schema
  []
  (d-schema/generate-schema datomic/tempid orsos-schema))
