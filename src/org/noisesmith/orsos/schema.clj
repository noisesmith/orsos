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
       [orsos-id    :string :indexed]
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
       [orsos-id                  :string :indexed]
       [committee-name            :string :indexed]
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
       [opposes-measures          :ref :many]
       [position                  :string]
       [candidate-office          :string]
       [candidate-office-group    :string]))
   (d-schema/schema trans-subtype
     (d-schema/fields
       [name      :string :indexed]
       [direction :enum [:in :out]]
       [type      :string :indexed]))
   (d-schema/schema transaction
     (d-schema/fields
       [id-str                    :string]
       [original-id               :string]
       [transaction-date          :instant :indexed]
       [status                    :enum [:original :amended]]
       [transaction-subtype       :ref :indexed]
       [amount                    :bigdec]
       [aggregate-amount          :bigdec]
       [filer-raw :string]
       [filer :ref "Reference to committee that filed this transaction"]
       [contributor-payee
        :ref "Contributor Payee - maybe a committee too"]
       [attest-by                 :ref]
       [attest-date               :instant]
       [review-by                 :ref]
       [review-date               :instant]
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

(def committee-lookup
  {:ref {nil 0
         :committee/treasurer 1
         :committee/treasurer-mailing-address 2
         :committee/candidate 3
         :committee/candidate-mailing-address 4}
   "Committee Id" {0 :committee/orsos-id}
   "Committee Name" {0 :committee/committee-name}
   "Committee Type" {0 :committee/committee-type}
   "Committee SubType" {0 :committee/committee-subtype}
   "Candidate Office" {0 :committee/candidate-office}
   "Candidate Office Group" {0 :committee/candidate-office-group}
   "Filing Date" {0 :committee/filing-date}
   "Organization Filing Date" {0 :committee/organization-filing-date}
   "Treasurer First Name" {1 :person/first-name}
   "Treasurer Last Name" {1 :person/last-name}
   "Treasurer Mailing Address" {2 :address/full}
   "Treasurer Work Phone" {1 :person/work-phone}
   "Treasurer Fax" {1 :person/fax-phone}
   "Candidate First Name" {3 :person/first-name}
   "Candidate Last Name" {3 :person/last-name}
   "Candidate Maling Address" {4 :address/full}
   "Candidate Work Phone" {3 :person/work-phone}
   "Candidate Residence Phone" {3 :person/residence-phone}
   "Candidate Fax" {3 :person/fax-phone}
   "Candidate Email" {3 :person/email}
   "Active Election" {0 :committee/active-election}
   "Measure" {0 :committee/position}})

(def transaction-lookup
  {:ref {nil 0
         :transaction/transaction-subtype 2
         :transaction/filer 1
         :transaction/attest-by 3
         :transaction/review-by 4
         :transaction/contributor-payee-agent 5
         :transaction/contributor-payee-address 6}
   "Tran Id" {0 :transaction/id-str}
   "Original Id" {0 :transaction/original-id}
   "Tran Date" {0 :transaction/transaction-date}
   "Tran Status" {0 :transaction/status}
   "Filer" {0 :transaction/filer-raw}
   "Contributor/Payee" {5 :entity/name}
   "Sub Type" {2 :trans-subtype/type}
   "Amount" {0 :transaction/amount}
   "Aggregate Amount" {0 :transaction/aggregate-amount}
   "Contributor/Payee Committee ID" {5 :entity/orsos-id}
   "Filer Id" {1 :committee/orsos-id}
   "Attest By Name" {3 :entity/name}
   "Attest Date" {0 :transaction/attest-date}
   "Review By Name" {4 :entity/name}
   "Review Date" {0 :transaction/review-date}
   "Due Date" {0 :transaction/due-date}
   "Occptn Ltr Date" {0 :transaction/occupation-letter-date}
   "Pymt Sched Txt" {0 :transaction/payment-schedule}
   "Purp Desc" {0 :transaction/purpose-description}
   "Intrst Rate" {0 :transaction/interest-rate}
   "Check Nbr" {0 :transaction/check-number}
   "Tran Stsfd Ind" {0 :transaction/is-trans-stsfd}
   "Filed By Name" {1 :committee/committee-name}
   "Filed Date" {0 :transaction/filed-date}
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
   "County" {0 :transaction/contributor-payee-county}
   "Purpose Codes" {0 :transaction/purpose-codes}
   "Exp Date" {0 :transaction/expired-date}})

(defn get-schema
  []
  (d-schema/generate-schema datomic/tempid orsos-schema))
