(ns ecommerce.model
  (:require [schema.core :as s]))

(defn uuid [] (java.util.UUID/randomUUID))

(def Categoria
  {:categoria/id   java.util.UUID
   :categoria/nome s/Str})

(def Produto
  {(s/optional-key :produto/nome)          s/Str
   (s/optional-key :produto/slug)          s/Str
   (s/optional-key :produto/preco)         BigDecimal
   :produto/id                             java.util.UUID
   (s/optional-key :produto/palavra-chave) [s/Str]
   (s/optional-key :produto/categoria)     Categoria})


(defn novo-produto
  ; gerar ids dinamicamente
  ([nome slug preco]
   (novo-produto (uuid) nome slug preco))
  ; usar ids que ja foram criados antes
  ([uuid nome slug preco]
   {:produto/id    uuid
    :produto/nome  nome
    :produto/slug  slug
    :produto/preco preco}))

(defn nova-categoria
  ([nome]
   (nova-categoria (uuid) nome))
  ([uuid nome]
   {:categoria/id   uuid
    :categoria/nome nome}))