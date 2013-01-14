(ns razorblade.core
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as string]))

(def ^{:dynamic true} *db* "postgres://localhost/razor-test")

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
               (map #(map-fn (:text %)))
               (string/join join-str))]
    {:text (if wrap-parens (str "(" text ")") text)
     :params (mapcat :params clauses)}))

(defn select [fields & clauses]
  (let [fields (if (coll? fields) fields [fields])]
    (combine-clauses (flatten [(to-clause "select")
                               (combine-clauses fields :join-str ", ")
                               clauses]))))

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

(defn exec [query]
  (clojure.java.jdbc/with-connection *db*
    (clojure.java.jdbc/with-query-results rows
      (vec (cons (:text query) (:params query)))
      (doall rows))))
