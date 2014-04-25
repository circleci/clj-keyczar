(ns clj-keyczar.keyset-test
  (require [clojure.test :refer :all]
           [clj-keyczar.keyset :as keyset]
           [clj-keyczar.crypt :as crypt]
           [clj-keyczar.sign :as sign]))

(deftest key-rotation-works-on-crypting-keysets
  (let [keyset (-> (keyset/create :crypt)
                   (keyset/addkey))]
    (let [ciphertext (crypt/encrypt keyset "narf")]
      ;; after demoting the only key from primary->active...
      (let [keyset (keyset/demote keyset 1)]
        ;; can't encrypt!
        (is (thrown? org.keyczar.exceptions.NoPrimaryKeyException
                     (crypt/encrypt keyset "narf")))
        ;; but can still decrypt!
        (is (= "narf" (crypt/decrypt keyset ciphertext)))
        ;; but cannot revoke
        (is (thrown? org.keyczar.exceptions.KeyczarException
                     (keyset/revoke keyset 1)))

        ;; after demoting it again, from active->inactive...
        (let [keyset (keyset/demote keyset 1)]
          ;; still can't encrypt!
          (is (thrown? org.keyczar.exceptions.NoPrimaryKeyException
                       (crypt/encrypt keyset "narf")))
          ;; and can still decrypt!
          (is (= "narf" (crypt/decrypt keyset ciphertext)))

          ;; now, can revoke!
          (let [keyset (keyset/revoke keyset 1)]
            ;; the original key is now revoked: should not be possible to decrypt old ciphertext
            (is (thrown? org.keyczar.exceptions.KeyNotFoundException
                         (crypt/decrypt keyset ciphertext)))

            ;; add a new primary
            (let [keyset (keyset/addkey keyset)]
              ;; we can round-trip plaintext again
              (is (= "narf" (crypt/decrypt keyset (crypt/encrypt keyset "narf"))))
              ;; and new ciphertext != old ciphertext
              (is (not= ciphertext (crypt/encrypt keyset "narf"))))))))))

(deftest key-rotation-works-on-signing-keysets
  (let [keyset (-> (keyset/create :sign)
                   (keyset/addkey))]
    (let [signature (sign/sign keyset "narf")]
      ;; after demoting the only key from primary->active...
      (let [keyset (keyset/demote keyset 1)]
        ;; can't sign!
        (is (thrown? org.keyczar.exceptions.NoPrimaryKeyException
                     (sign/sign keyset "narf")))
        ;; but can still verify!
        (is (sign/verify keyset "narf" signature))
        ;; but cannot revoke
        (is (thrown? org.keyczar.exceptions.KeyczarException
                     (keyset/revoke keyset 1)))

        ;; after demoting it again, from active->inactive...
        (let [keyset (keyset/demote keyset 1)]
          ;; still can't sign!
          (is (thrown? org.keyczar.exceptions.NoPrimaryKeyException
                       (sign/sign keyset "narf")))
          ;; and can still verify!
          (is (sign/verify keyset "narf" signature))

          ;; now, can revoke!
          (let [keyset (keyset/revoke keyset 1)]
            ;; the original key is now revoked: should not be possible to verify old signatures
            (is (thrown? org.keyczar.exceptions.KeyNotFoundException
                         (sign/verify keyset "narf" signature)))

            ;; add a new primary
            (let [keyset (keyset/addkey keyset)]
              ;; we can round-trip plaintext again
              (is (sign/verify keyset "narf" (sign/sign keyset "narf")))
              ;; and new signature != old signature
              (is (not= signature (sign/sign keyset "narf"))))))))))
