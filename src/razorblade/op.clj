(ns razorblade.op
  (:require [razorblade.core :refer [combine-clauses]])
  (:refer-clojure :exclude [and or - + > < =]))

(defn and [& stmts]
  (combine-clauses
    stmts
    :map-fn #(str "(" % ")")
    :join-str " AND "))

(defn or [& stmts]
  (combine-clauses
    stmts
    :map-fn #(str "(" % ")")
    :join-str " OR "))

(defn > [clause1 clause2]
  (combine-clauses
    [clause1 clause2]
    :join-str " > "))

(defn < [clause1 clause2]
  (combine-clauses
    [clause1 clause2]
    :join-str " < "))

(defn = [clause1 clause2]
  (combine-clauses
    [clause1 clause2]
    :join-str " = "))

(defn <> [clause1 clause2]
  (combine-clauses
    [clause1 clause2]
    :join-str " <> "))

(defn + [clause1 clause2]
  (combine-clauses
    [clause1 clause2]
    :join-str " + "))

(defn - [clause1 clause2]
  (combine-clauses
    [clause1 clause2]
    :join-str " - "))

(defn in [clause1 clause2]
  (combine-clauses
    [clause1 clause2]
    :join-str " in "))

(defn not-in [clause1 clause2]
  (combine-clauses
    [clause1 clause2]
    :join-str " not in "))

(defn is-null [clause]
  (combine-clauses
    [clause "is null"]))

(defn is-not-null [clause]
  (combine-clauses
    [clause "is not null"]))
