(ns razorblade.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]))

(defonce ^{:private true} default-db-spec (atom nil))

(defn set-default-db-spec [db-spec]
  (reset! default-db-spec db-spec))

(def ^{:dynamic true} *db-spec* nil)

(defn current-db-spec []
  (or *db-spec* @default-db-spec))

(declare combine-clauses)

(defn field [v]
  (cond
    (coll? v)    (combine-clauses (map field v) :wrap-parens true :join-str ",")
    :else        {:text "?"
                  :params [v]}))

(defn fieldify [k]
  (string/replace (name k) "-" "_"))

(defn to-clause [v]
  (cond
    (map? v)     v
    (coll? v)    (combine-clauses v :wrap-parens true :join-str ",")
    (keyword? v) {:text (fieldify v)
                  :params []}
    (nil? v)     nil
    :else        {:text (str v)
                  :params []}))

(def default-opts
  {:map-fn str
   :join-str " "
   :wrap-parens false})

(defn combine-clauses
  [clauses & opt-kvs]
  (let [{:keys [map-fn join-str wrap-parens]} (merge default-opts (apply hash-map opt-kvs))
        clauses (map to-clause clauses)
        text (->> clauses
               (remove nil?)
               (map #(map-fn (:text %)))
               (string/join join-str))]
    {:text (if wrap-parens (str "(" text ")") text)
     :params (mapcat :params clauses)}))

(defn select [fields & clauses]
  (let [fields (if (coll? fields) fields [fields])]
    (combine-clauses (flatten [(to-clause "select")
                               (combine-clauses fields :join-str ", ")
                               clauses]))))

(defn insert-into [table fields select-clause]
  (combine-clauses ["insert into" table fields select-clause]))

(defn update [table & clauses]
  (combine-clauses (concat ["update" table] clauses)))

(defn delete [& clauses]
  (combine-clauses (cons "delete" clauses)))

(defn set-fields [& clauses]
  (combine-clauses ["set"
                    (combine-clauses clauses :join-str ", ")]))

(defn from [clause]
  (combine-clauses ["from" clause]))

(defn join [table-clause on-clause]
  (combine-clauses ["inner join" table-clause "on" on-clause]))

(defn where [clause]
  (combine-clauses ["where" clause]))

(defn prefix [prefix fields]
  (->> fields
    (map to-clause)
    (map #(update-in % [:text] (partial str (name prefix) ".")))))

(defn exec [q]
  (let [q (to-clause q)])
    (jdbc/with-connection (current-db-spec)
      (first (jdbc/do-prepared
        (:text q)
        (:params q)))))

(defn query [q]
  (let [q (to-clause q)])
    (jdbc/with-connection (current-db-spec)
      (jdbc/with-query-results rows
        (vec (cons (:text q) (:params q)))
        (doall rows))))

(defn insert [table & records]
  (let [table (fieldify table)]
    (jdbc/with-connection (current-db-spec)
      (apply jdbc/insert-records table records))))
