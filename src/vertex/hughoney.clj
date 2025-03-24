(ns vertex.hughoney
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql]
            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [honey.sql :as sql])
  (:import (java.util UUID)))

(def db {:dbtype       "sqlite"
         :dbname       "clojure.sqlite"
         :busy-timeout 10000})

(def ds (jdbc/get-datasource db))

(jdbc/execute! ds ["
create table if not exists address (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  email TEXT NOT NULL
)"])

(defn create-record []
  (let [r (str (UUID/randomUUID))]
    (with-open [conn (jdbc/get-connection ds)]
      (next.jdbc.sql/insert! conn :address
                             {:name  (str "name-" r)
                              :email (str r "@email.com")}))))

(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))

(def db-fns (hugsql/map-of-db-fns "vertex/hughoney.sql"))

(defn fetch-address-by [& {:keys [id email name]}]
  (with-open [conn (jdbc/get-connection ds)]
    ((-> db-fns :fetch-address :fn)
     conn
     ;; Dynamic part of query, in this simple example we already have
     ;; many possible combinations which is hard to handle in static query
     {:cond (sql/format [:and
                         (when id [:= :id id])
                         (when email [:like :email (str "%" email "%")])
                         (when name [:like :name (str "%" name "%")])])})))

;; generate some data
(dotimes [_ 100]
  (create-record))

;; fetch by name and email
(fetch-address-by :email ".com" :name "na")

;; fetch by id
(fetch-address-by :id 1)
