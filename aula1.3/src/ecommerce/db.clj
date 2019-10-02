(ns ecommerce.db
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.model :as model]
            [schema.core :as s]))

(def db-uri "datomic:dev://localhost:4334/ecommerce")

(defn abre-conexao! []
  (d/create-database db-uri)
  (d/connect db-uri))

(defn apaga-banco! []
  (d/delete-database db-uri))

; Produtos
; id?
; nome String 1 ==> Computador Novo
; slug String 1 ==> /computador_novo
; preco ponto flutuante 1 ==> 3500.10
; categoria_id integer ==> 3

; id_entidade atributo valor
; 15 :produto/nome Computador Novo     ID_TX     operacao
; 15 :produto/slug /computador_novo    ID_TX     operacao
; 15 :produto/preco 3500.10    ID_TX     operacao
; 15 :produto/categoria 37
; 17 :produto/nome Telefone Caro    ID_TX     operacao
; 17 :produto/slug /telefone    ID_TX     operacao
; 17 :produto/preco 8888.88    ID_TX     operacao

; 36 :categoria/nome Eletronicos

(def schema [
             ; Produtos
             {:db/ident       :produto/nome
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "O nome de um produto"}
             {:db/ident       :produto/slug
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "O caminho para acessar esse produto via http"}
             {:db/ident       :produto/preco
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one
              :db/doc         "O preço de um produto com precisão monetária"}
             {:db/ident       :produto/palavra-chave
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/many}
             {:db/ident       :produto/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :produto/categoria
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}

             ; Categorias
             {:db/ident       :categoria/nome
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :categoria/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}

             ; Transações
             {:db/ident       :tx-data/ip
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}

             ])



(s/defn adiciona-produtos!
  ([conn, produtos :- [model/Produto]]
   (d/transact conn produtos))
  ([conn, produtos :- [model/Produto], ip]
   (let [db-add-ip [:db/add "datomic.tx" :tx-data/ip ip]]
     (d/transact conn (conj produtos db-add-ip)))))




(defn cria-schema! [conn]
  (d/transact conn schema))

; pull generico, vantagem preguica, desvantagem pode trazer mais do que eu queira
(defn todos-os-produtos [db]
  (d/q '[:find (pull ?entidade [*])
         :where [?entidade :produto/nome]] db))

(def todos-os-produtos-por-slug-fixo-q
  '[:find ?entidade
    :where [?entidade :produto/slug "/computador-novo"]])

(defn todos-os-produtos-por-slug-fixo [db]
  (d/q todos-os-produtos-por-slug-fixo-q db))

; não estou usando notacao hungara e extract
; eh comum no sql: String sql = "select * from where slug=::SLUG::"
; conexao.query(sql, {::SLUG:: "/computador-novo})
(defn todos-os-produtos-por-slug [db slug]
  (d/q '[:find ?entidade
         :in $ ?slug-procurado                              ; proposital diferente da variável de clojure para evitar erros
         :where [?entidade :produto/slug ?slug-procurado]]
       db slug))


; ?entity => ?entidade => ?produto => ?p
; se não vai usar... _
(defn todos-os-slugs [db]
  (d/q '[:find ?slug
         :where [_ :produto/slug ?slug]] db))

; estou sendo explicito nos campos 1 a 1
(defn todos-os-produtos-por-preco [db preco-minimo-requisitado]
  (d/q '[:find ?nome, ?preco
         :in $, ?preco-minimo
         :keys produto/nome, produto/preco
         :where [?produto :produto/preco ?preco]
         [(> ?preco ?preco-minimo)]
         [?produto :produto/nome ?nome]]
       db, preco-minimo-requisitado))

(defn todos-os-produtos-por-palavra-chave [db palavra-chave-buscada]
  (d/q '[:find (pull ?produto [*])
         :in $ ?palavra-chave
         :where [?produto :produto/palavra-chave ?palavra-chave]]
       db palavra-chave-buscada))


(defn um-produto-por-dbid [db db-id]
  (d/pull db '[*] db-id))

(defn um-produto [db produto-id]
  (d/pull db '[*] [:produto/id produto-id]))


(defn db-adds-de-atribuicao-de-categorias [produtos categoria]
  (reduce (fn [db-adds produto] (conj db-adds [:db/add
                                               [:produto/id (:produto/id produto)]
                                               :produto/categoria
                                               [:categoria/id (:categoria/id categoria)]]))
          []
          produtos))

(defn atribui-categorias! [conn produtos categoria]
  (let [a-transacionar (db-adds-de-atribuicao-de-categorias produtos categoria)]
    (d/transact conn a-transacionar)))


; como esses dois estão genéricos poderiam ser um só
; mas vamos manter dois pois se você usa schema fica mais fácil de trabalhar
(s/defn adiciona-categorias! [conn, categorias :- [model/Categoria]]
  (d/transact conn categorias))

(defn todos-os-nomes-de-produtos-e-categorias [db]
  (d/q '[:find ?nome-do-produto ?nome-da-categoria
         :keys produto categoria
         :where [?produto :produto/nome ?nome-do-produto]
         [?produto :produto/categoria ?categoria]
         [?categoria :categoria/nome ?nome-da-categoria]]
       db))

; exemplo com backward navigation
; >>>>>>>>>> :produto/_categoria ?categoria
(defn todos-os-produtos-da-categoria [db nome-da-categoria]
  (d/q '[:find (pull ?categoria [:categoria/nome {:produto/_categoria [:produto/nome :produto/slug]}])
         :in $ ?nome
         :where [?categoria :categoria/nome ?nome]]
       db nome-da-categoria))

(defn resumo-dos-produtos [db]
  (d/q '[:find (min ?preco) (max ?preco) (count ?preco) (sum ?preco)
         :keys minimo maximo quantidade preco-total
         :with ?produto
         :where [?produto :produto/preco ?preco]]
       db))

(defn resumo-dos-produtos-por-categoria [db]
  (d/q '[:find ?nome (min ?preco) (max ?preco) (count ?preco) (sum ?preco)
         :keys categoria minimo maximo quantidade preco-total
         :with ?produto
         :where [?produto :produto/preco ?preco]
         [?produto :produto/categoria ?categoria]
         [?categoria :categoria/nome ?nome]]
       db))

; queremos fazer as duas queries de uma vez só
; podemos criar nested queries
(defn todos-os-produtos-mais-caros [db]
  (d/q '[:find (pull ?produto [*])
         :where [(q '[:find (max ?preco)
                      :where [_ :produto/preco ?preco]]
                    $) [[?preco]]]
         [?produto :produto/preco ?preco]]
       db))

(defn todos-os-produtos-mais-baratos [db]
  (d/q '[:find (pull ?produto [*])
         :where [(q '[:find (min ?preco)
                      :where [_ :produto/preco ?preco]]
                    $) [[?preco]]]
         [?produto :produto/preco ?preco]]
       db))

(defn todos-os-produtos-do-ip [db ip]
  (d/q '[:find (pull ?produto [*])
         :in $ ?ip-buscado
         :where [?transacao :tx-data/ip ?ip-buscado]
         [?produto :produto/id _ ?transacao]]
       db ip))


; versão simples que não faz recursivamente
(defn datomic-para-entidade [entidades]
  (map #(dissoc % :db/id) entidades))

(s/defn todas-as-categorias :- [model/Categoria] [db]
  (datomic-para-entidade
    (d/q '[:find [(pull ?categoria [*]) ...]
           :where [?categoria :categoria/id]]
         db)))

(defn cria-dados-de-exemplo [conn]
  (def eletronicos (model/nova-categoria "Eletrônicos"))
  (def esporte (model/nova-categoria "Esporte"))
  (pprint @(adiciona-categorias! conn [eletronicos, esporte]))

  (def computador (model/novo-produto (model/uuid) "Computador Novo", "/computador-novo", 2500.10M))
  (def celular (model/novo-produto (model/uuid) "Celular Caro", "/celular", 888888.10M))
  ;(def calculadora {:produto/nome "Calculadora com 4 operações"})
  (def celular-barato (model/novo-produto "Celular Barato", "/celular-barato", 0.1M))
  (def xadrez (model/novo-produto "Tabuleiro de xadrez", "/tabuleiro-de-xadrez", 30M))
  (pprint @(adiciona-produtos! conn [computador, celular, celular-barato, xadrez] "200.216.222.125"))

  (atribui-categorias! conn [computador, celular, celular-barato] eletronicos)
  (atribui-categorias! conn [xadrez] esporte))
