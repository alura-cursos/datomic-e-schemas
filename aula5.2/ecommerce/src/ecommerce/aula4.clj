(ns ecommerce.aula4
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

(pprint (db/todos-os-produtos-com-estoque (d/db conn)))

(def produtos (db/todos-os-produtos (d/db conn)))
(pprint (db/um-produto-com-estoque (d/db conn) (:produto/id (first produtos))))
(pprint (db/um-produto-com-estoque (d/db conn) (:produto/id (second produtos))))