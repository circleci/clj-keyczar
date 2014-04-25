(ns clj-keyczar.keyset
  (:require [clojure.data.json :as json])
  (:import org.keyczar.interfaces.KeyczarReader
           org.keyczar.enums.KeyPurpose
           org.keyczar.enums.KeyStatus
           org.keyczar.DefaultKeyType
           org.keyczar.GenericKeyczar
           org.keyczar.Keyczar
           org.keyczar.KeyMetadata))

(defn ->KeyczarReader
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

(defn- ->GenericKeyczar
  [keyset]
  (GenericKeyczar. ^KeyczarReader (->KeyczarReader keyset)))

(defn create
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

(defn- Keyczar->keyset
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
  [keyset]
  (let [generic-keyczar (->GenericKeyczar keyset)]
    (.addVersion ^GenericKeyczar generic-keyczar KeyStatus/PRIMARY)
    (Keyczar->keyset generic-keyczar)))

(defn demote
  [keyset version]
  (let [generic-keyczar (->GenericKeyczar keyset)]
    (.demote ^GenericKeyczar generic-keyczar (int version))
    (Keyczar->keyset generic-keyczar)))

(defn revoke
  [keyset version]
  (let [generic-keyczar (->GenericKeyczar keyset)]
    (.revoke ^GenericKeyczar generic-keyczar (int version))
    (Keyczar->keyset generic-keyczar)))
