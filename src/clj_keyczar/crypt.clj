(ns clj-keyczar.crypt
  "Functions for encrypting and decrypting data."
  (:require [clj-keyczar.keyset :as keyset])
  (:import org.keyczar.Crypter
           org.keyczar.interfaces.KeyczarReader))

(defn ->crypter
  "Tries to coerce its argument into a Crypter. Knows how to cope with Crypter, keyset path
  (string), keyset (map) and KeyczarReader inputs."
  [crypter-ish]
  (cond
    (isa? (type crypter-ish) Crypter) crypter-ish
    (isa? (type crypter-ish) KeyczarReader) (Crypter. ^KeyczarReader crypter-ish)
    (string? crypter-ish) (Crypter. ^String crypter-ish)
    (map? crypter-ish) (Crypter. ^KeyczarReader (keyset/->KeyczarReader crypter-ish))
    :else (throw (IllegalArgumentException.
                   (str "Can't coerce " (type crypter-ish) " into a Crypter")))))

(defonce ^:dynamic *crypter* nil)

(defn encrypt
  "Encrypt a chunk of data, using the provided crypter (or *crypter* by default.) Returns
  ciphertext (a string.)"
  ([crypter-ish data]
   (.encrypt ^Crypter (->crypter crypter-ish) data))
  ([data]
   (assert *crypter*)
   (encrypt *crypter* data)))

(defn decrypt
  "Decrypt a ciphertext, using the provided crypter (or *crypter* by default.) Returns
  cleartext (a string.)"
  ([crypter-ish ciphertext]
   (.decrypt ^Crypter (->crypter crypter-ish) ciphertext))
  ([ciphertext]
   (assert *crypter*)
   (decrypt *crypter* ciphertext)))

(defn set-crypter!
  "Given a crypter-ish (see ->crypter), sets *crypter*."
  [crypter-ish]
  (alter-var-root #'*crypter* (fn [& _] (->crypter crypter-ish))))

(defmacro with-crypter
  "Given a crypter-ish (see ->crypter), runs the body with *crypter* bound to the
  appropriate Crypter."
  [crypter-ish & body]
  `(binding [*crypter* (->crypter ~crypter-ish)]
     ~@body))
