(ns clj-keyczar.keyset
  "Functions for working with a keyset as a clojure map."
  (:require [clojure.data.json :as json])
  (:import org.keyczar.interfaces.KeyczarReader
           org.keyczar.enums.KeyPurpose
           org.keyczar.enums.KeyStatus
           org.keyczar.DefaultKeyType
           org.keyczar.GenericKeyczar
           org.keyczar.Keyczar
           org.keyczar.KeyMetadata))

(defn ->KeyczarReader
  "Implements the KeyczarReader interface over a keyset represented as a map. This is
  how map keysets interface with the rest of the keyczar java library."
  [keyset]
  (reify KeyczarReader
    (getKey [this n]
      (json/write-str (get-in keyset [:keys n])))

    (getKey [this]
      (.getKey this
               (-> keyset
                   :meta
                   (json/write-str)
                   (KeyMetadata/read)
                   .getPrimaryVersion
                   .getVersionNumber)))

    (getMetadata [this]
      (json/write-str (:meta keyset)))))

(defn create
  "Create and return a new keyset. The purpose can be either :crypt or :sign, and determines
  both what the keyset can be used for (clj-keyset.crypt/* and clj-keyset.sign/*,
  respectively) and the key type that will be contained in the keyset (AES and HMAC_SHA1,
  respectively.)"
  [purpose-kw]
  (let [[purpose type] (case purpose-kw
                         ;; n.b. this is minimalist: see org.keyczar.KeyczarTool for all the
                         ;; stuff this *doesn't* support...
                         :crypt [KeyPurpose/DECRYPT_AND_ENCRYPT DefaultKeyType/AES]
                         :sign [KeyPurpose/SIGN_AND_VERIFY DefaultKeyType/HMAC_SHA1]
                         (throw (Exception. (str "Unknown keyset purpose: " purpose-kw))))]
    {:meta (-> (KeyMetadata. "" purpose type)
               (str)
               (json/read-str))
     :keys {}}))

(defn- ->GenericKeyczar
  "Builds a GenericKeyczar out of a keyset represented as a map. This should only be needed
  internally, to be able to use the GenericKeyczar methods for adding/demoting/revoking keys."
  [keyset]
  (GenericKeyczar. ^KeyczarReader (->KeyczarReader keyset)))

(defn- Keyczar->keyset
  "Turns a Keyczar object back into a map keyset. This should only be needed internally,
  to convert back into a map after manipulating a GenericKeyczar."
  [keyczar]
  {:meta (-> keyczar
             (str)
             (json/read-str))
   :keys (->> keyczar
              (.getVersions)
              (mapcat (fn [v]
                        [(.getVersionNumber v) (-> (.getKey keyczar v) str (json/read-str))]))
              (apply hash-map))})

(defn addkey
  "Add a key to a keyset. The newly added key will become the primary. If there's already a
  primary key in the keyset, it will be implicitly demoted to active. Does not modify its
  input: returns a new keyset."
  [keyset]
  (let [generic-keyczar (->GenericKeyczar keyset)]
    (.addVersion ^GenericKeyczar generic-keyczar KeyStatus/PRIMARY)
    (Keyczar->keyset generic-keyczar)))

(defn promote
  "Promote a specified key. An active key will be promoted to primary; an inactive key will
  be made active. Does not modify its inputs: returns a new keyset."
  [keyset version]
  (let [generic-keyczar (->GenericKeyczar keyset)]
    (.promote ^GenericKeyczar generic-keyczar (int version))
    (Keyczar->keyset generic-keyczar)))

(defn demote
  "Demote a specified key. A primary key will be demoted to active; an active key will be
  made inactive. Does not modify its inputs: returns a new keyset."
  [keyset version]
  (let [generic-keyczar (->GenericKeyczar keyset)]
    (.demote ^GenericKeyczar generic-keyczar (int version))
    (Keyczar->keyset generic-keyczar)))

(defn revoke
  "Revoke a specified key. Only an inactive key can be revoked! Does not modify its inputs:
  returns a new keyset."
  [keyset version]
  (let [generic-keyczar (->GenericKeyczar keyset)]
    (.revoke ^GenericKeyczar generic-keyczar (int version))
    (Keyczar->keyset generic-keyczar)))
