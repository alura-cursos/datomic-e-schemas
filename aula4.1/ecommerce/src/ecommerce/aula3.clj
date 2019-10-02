(ns ecommerce.aula3
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.db :as db]
            [ecommerce.model :as model]
            [schema.core :as s]))

(s/set-fn-validation! true)

(db/apaga-banco!)
(def conn (db/abre-conexao!))
(db/cria-schema! conn)
(db/cria-dados-de-exemplo conn)

(def produtos (db/todos-os-produtos (d/db conn)))
(def primeiro-produto (first produtos))
(pprint primeiro-produto)

; se não encontra, devolve nil. independentemente de schema ativo ou não
(pprint (db/um-produto (d/db conn) (:produto/id primeiro-produto)))
(pprint (db/um-produto (d/db conn) (model/uuid)))

; se não encontra, joga um erro. independentemente de schema ativo ou não
(pprint (db/um-produto! (d/db conn) (:produto/id primeiro-produto)))
(pprint (db/um-produto! (d/db conn) (model/uuid)))
