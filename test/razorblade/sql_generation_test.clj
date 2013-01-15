(ns razorblade.sql-generation-test
  (:use clojure.test
        razorblade.core)
  (:require [razorblade.op :as op]))

(def user {:id 4})
(def excluded-categories [1 2 3 4])

(deftest gnarly-select-query
  (let [result (select (prefix :dingos [:id :name])
                       (from :dingos)
                       (join :dingo-facts (op/= :dingos.id :dingo-facts.dingo-id))
                       (where
                         (op/or
                           (op/and
                             "dingo_facts.created_at > (NOW() - INTERVAL '1 DAY')"
                             (op/> :dingo-facts.updated-at "NOW() - INTERVAL '1 DAY'")
                             (when false "you should not see this")
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

(deftest delete-query
  (let [result (delete (from :living-dingos)
                       (where
                         (op/or
                           (op/<> :status (field "living"))
                           (op/< :deceased-at "now()"))))]
    (is (= (:text result)
           "delete from living_dingos where (status <> ?) OR (deceased_at < now())"))
    (is (= (:params result)
           ["living"]))))

(deftest update-query
  (let [result (update :living-dingos
                  (set-fields (op/= :status (field "deceased"))
                              (op/= :deceased-by-id 4))
                  (where (op/is-not-null :deceased-at)))]
    (is (= (:text result)
           "update living_dingos set status = ?, deceased_by_id = 4 where deceased_at is not null"))
    (is (= (:params result)
           ["deceased"]))))

(deftest insert-into-query
  (let [result (insert-into :living-dingos [:name :spottings]
                 (select [:name 0]
                   (from :dingos)
                   (where (op/<> :status (field "deceased")))))]
    (is (= (:text result)
           "insert into living_dingos (name,spottings) select name, 0 from dingos where status <> ?"))
    (is (= (:params result)
           ["deceased"]))))
