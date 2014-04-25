(ns clj-keyczar.sign
  "Functions for signing and validating data."
  (:require [clj-keyczar.keyset :as keyset])
  (:import org.keyczar.Signer
           org.keyczar.interfaces.KeyczarReader))

(defn ->signer
  "Tries to coerce its argument into a Signer. Knows to to cope with Signer, string,
  keyset (map) and KeyczarReader inputs."
  [signer-ish]
  (cond
    (isa? (type signer-ish) Signer) signer-ish
    (isa? (type signer-ish) KeyczarReader) (Signer. ^KeyczarReader signer-ish)
    (string? signer-ish) (Signer. ^String signer-ish)
    (map? signer-ish) (Signer. ^KeyczarReader (keyset/->KeyczarReader signer-ish))
    :else (throw (IllegalArgumentException.
                   (str "Can't coerce " (type signer-ish) " into a Signer")))))

(defonce ^:dynamic *signer* nil)

(defn sign
  "Sign a message, using the provided signer (or *signer* by default.) Returns a signature."
  ([signer-ish message]
   (.sign ^Signer (->signer signer-ish) message))
  ([message]
   (assert *signer*)
   (sign *signer* message)))

(defn verify
  "Verify that a given message and signature match, using the provided signer (or *signer*
  by default.) Returns a boolean."
  ([signer-ish message signature]
   (.verify ^Signer (->signer signer-ish) message signature))
  ([message signature]
   (assert *signer*)
   (verify *signer* message signature)))

(defn set-signer!
  "Given a signer-ish (see ->signer), sets *signer*."
  [signer-ish]
  (alter-var-root #'*signer* (fn [& _] (->signer signer-ish))))

(defmacro with-signer
  "Given a signer-ish (see ->signer), runs the body with *signer* bound to the appropriate
  Signer."
  [signer-ish & body]
  `(binding [*signer* (->signer ~signer-ish)]
     ~@body))
