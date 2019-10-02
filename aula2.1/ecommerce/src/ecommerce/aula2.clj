(ns ecommerce.aula2
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

(pprint (db/todas-as-categorias (d/db conn)))
(pprint (db/todos-os-produtos (d/db conn)))

(def dama {:produto/nome  "Dama"
      :produto/slug  "/dama"
      :produto/preco 15.5M
      :produto/id    (model/uuid)} )

; update/insert ==> upsert
(db/adiciona-ou-altera-produtos! conn [dama])
(pprint (db/um-produto (d/db conn) (:produto/id dama)))

(db/adiciona-ou-altera-produtos! conn [(assoc dama :produto/slug "/jogo-de-dama")])
(pprint (db/um-produto (d/db conn) (:produto/id dama)))

(db/adiciona-ou-altera-produtos! conn [(assoc dama :produto/preco 150.5M)])
(pprint (db/um-produto (d/db conn) (:produto/id dama)))

; detectamos um problema que é uma dificuldade... entender que updates sobrescreverão campos e operações anteriores







