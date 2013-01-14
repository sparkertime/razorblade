# razorblade

A SQL library for Clojure designed to be primitive yet composable. As with any sharp object, take care not to cut yourself during use.

## Motivation

Razorblade is designed to stay as close to raw SQL as possible and yet provide composable interfaces. If you've found yourself using _clojure.java.jdbc_ and tiring of string manipulation, or using Korma and straining when writing gnarlier queries or deviating from the Korma happy path, this library may be for you.

Razorblade's goals are as follows:

* Inserting raw SQL in any part of your query should be extremely simple.
* Conditionally including/excluding parts of a query should be extremely simple.
* Support MySQL and Postgres DB querying (others welcome too but not a strict goal of this).

Korma, ClojureQL, and native clojure.java.jdbc are great options and you should investigate them before deciding whether Razorblade is right for you.

Razorblade _may not_ be for you if you experience and of the following symptoms:

* You only build simple queries (use Korma or clojure.java.jdbc)
* You don't know SQL that well or desire a non-SQL representation of querying (use Korma or ClojureQL respectively)
* You want a library that can intelligently build SQL based on schema / entity relationships. (use Korma)

## Usage

Not recommended just yet.

All functions are built to return a map of _:text_ and _:params_. Arguments can be virtually anything and will be coerced as appropriate.

```clj
; building simple queries is straightforward.
(select :name (from :dingos))
;yields
{:text "select name from dingos" :params []}
```

```clj
; keywords are coerced to db-representative table/field names
(select [:dingos.baby-eaten-count :dingos.name] (from :dingos))
;yields
{:text "select dingos.baby_eaten_count, dingos.name from dingos" :params []}
```

```clj
; logical operators live in the op namespace to avoid conflicts with their Clojure equivalents
(select :*
  (from :dingos)
  (where (op/and
           (op/= :baby-eaten-count 5)
           (op/is-null :perished-at))))
;yields
{:text "select * from dingos where baby_eaten_count = 5 AND perished_at IS NULL" :params []}
```

```clj
; strings and user input should go through the field function
(select :*
  (from :dingos)
  (where (op/and
           (op/= :baby-eaten-count (field 5))
           (op/in :name (field ["richard" "blueberry"])))))
;yields
{:text "select * from dingos where baby_eaten_count = ? AND name in (?,?)" :params [5 "richard" "blueberry"]}
```

```clj
; raw sql is always welcome
(select :*
  (from :dingos)
  (where (op/and
           (op/< :perished-at "now()")
           (op/in :name (field ["richard" "blueberry"])))))
;yields
{:text "select * from dingos where perished_at < now() AND name in (?,?)" :params [5 "richard" "blueberry"]}

;this does the same thing
(select :*
  (from :dingos)
  (where (op/and
           "perished_at < now()"
           (op/in :name (field ["richard" "blueberry"])))))
```

```clj
; nil clauses get removed
(select :*
  (from :dingos)
  (where (op/and
           (when false (op/= :baby-eaten-count (field 5)))
           (op/in :name (field ["richard" "blueberry"])))))
;yields
{:text "select * from dingos where name in (?,?)" :params ["richard" "blueberry"]}
```

```clj
; executing queries is straightforward too
(exec (select :name (from :dingos)))

; and of course you can always pass in your own. The above is equivalent to
(exec {:text "select name from from dingos" :params []})
```

```clj
; This scales to more intricate queries
  (select (prefix :dingos [:id :name])
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
      (op/not-in :dingo-facts.category (field excluded-categories)))))
;yields
{:text "select dingos.id, dingos.name from dingos inner join dingo_facts on dingos.id = dingo_facts.dingo_id where ((dingo_facts.created_at > (NOW() - INTERVAL '1 DAY')) AND (dingo_facts.updated_at > NOW() - INTERVAL '1 DAY') AND (dingo_facts.last_seen_at > now() - INTERVAL '1 DAY')) AND (dingo_facts.user_id = 4) AND (dingo_facts.category in (1,2,3,4))) OR ((user_id = 4) AND (dingo_facts.category not in (1,2,3,4)))"
 :params []}
```

## Todo
* Support Update, Delete queries
* Provide for object sanitization in style of ```as-fields [args] body``` to avoid ```(field ...)``` calls
* Provide Korma-style before/after transformation examples (should stay separate from exec calls though. Thinking another ```with-*``` wrapper

## License

Copyright © 2013 Scott Parker

Distributed under the Eclipse Public License, the same as Clojure.
