(ns razorblade.sql-generation-test
  (:use clojure.test
        razorblade.core)
  (:require [razorblade.op :as op]))

(def user {:id 4})
(def excluded-categories [1 2 3 4])

(deftest gnarly-query
  (let [result (select (prefix :dingos [:id :name])
                       (from :dingos)
                       (join :dingo-facts (op/= :dingos.id :dingo-facts.dingo-id))
                       (where
                         (op/or
                           (op/and
                             "dingo_facts.created_at > (NOW() - INTERVAL '1 DAY')"
                             (op/> :dingo-facts.updated-at "NOW() - INTERVAL '1 DAY'")
                             (op/> :dingo-facts.last-seen-at (op/- "now()" "INTERVAL '1 DAY')"))
                             (op/= :dingo-facts.user-id (:id user))
                             (op/in :dingo-facts.category excluded-categories))
                           (op/and
                             (op/= :user-id (:id user))
                             (op/not-in :dingo-facts.category (rest excluded-categories))))))]
    (is (= (:text result)
           "select dingos.id, dingos.name from dingos inner join dingo_facts on dingos.id = dingo_facts.dingo_id where ((dingo_facts.created_at > (NOW() - INTERVAL '1 DAY')) AND (dingo_facts.updated_at > NOW() - INTERVAL '1 DAY') AND (dingo_facts.last_seen_at > now() - INTERVAL '1 DAY')) AND (dingo_facts.user_id = 4) AND (dingo_facts.category in (1,2,3,4))) OR ((user_id = 4) AND (dingo_facts.category not in (2,3,4)))"))
    (is (= (:params result)
           []))))
