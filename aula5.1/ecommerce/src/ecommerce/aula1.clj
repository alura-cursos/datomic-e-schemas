(ns ecommerce.aula1
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.db :as db]
            [ecommerce.model :as model]
            [schema.core :as s]))

(s/set-fn-validation! true)

(db/apaga-banco!)
(def conn (db/abre-conexao!))
(db/cria-schema! conn)

(defn testa-schema []
  (def computador (model/novo-produto (model/uuid) "Computador Novo", "/computador-novo", 2500.10M))
  (pprint (s/validate model/Produto computador))
  ;(pprint (s/validate model/Produto (assoc computador :produto/preco 76)))

  (def eletronicos (model/nova-categoria "Eletrônicos"))
  (pprint (s/validate model/Categoria eletronicos))
  (pprint (s/validate model/Produto (assoc computador :produto/categoria eletronicos))))

(testa-schema)

(db/cria-dados-de-exemplo conn)

(pprint (db/todas-as-categorias (d/db conn)))
(pprint (db/todos-os-produtos (d/db conn)))





