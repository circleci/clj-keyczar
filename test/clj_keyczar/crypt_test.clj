(ns clj-keyczar.crypt-test
  (:require [clojure.test :refer :all]
            [clj-keyczar.keyset :as keyset]
            [clj-keyczar.crypt :as crypt]))

(deftest ->crypter-works
  (testing "attempts to coerce a path into a crypter"
    (is (thrown-with-msg? org.keyczar.exceptions.KeyczarException #"Error reading file"
                          (crypt/->crypter "/blahbitty-nonexistent"))))
  (let [keyset (keyset/create :crypt)]
    (testing "coerces a keyset into a crypter"
      (is (isa? (type (crypt/->crypter keyset)) org.keyczar.Crypter)))
    (testing "coerces a KeyczarReader into a crypter"
      (is (isa? (type (crypt/->crypter (keyset/->KeyczarReader keyset))) org.keyczar.Crypter)))
    (let [crypter (crypt/->crypter keyset)]
      (testing "noops things that are already crypters"
        (is (= crypter (crypt/->crypter crypter))))))
  (testing "otherwise throws"
    (is (thrown? IllegalArgumentException (crypt/->crypter 9)))))

(deftest encrypting-and-decrypting-work
  (testing "happy path works"
    (let [keyset (-> (keyset/create :crypt)
                     (keyset/addkey))]
      (is (= "foo" (crypt/decrypt keyset (crypt/encrypt keyset "foo"))))))
  (testing "different crypters are incompatible"
    (let [ks1 (-> (keyset/create :crypt)
                  (keyset/addkey))
          ks2 (-> (keyset/create :crypt)
                  (keyset/addkey))
          plain "foo"
          cipher1 (crypt/encrypt ks1 plain)
          cipher2 (crypt/encrypt ks2 plain)]
      (is (not= cipher1 cipher2))
      (is (thrown? org.keyczar.exceptions.KeyNotFoundException
                   (crypt/decrypt ks1 cipher2)))
      (is (thrown? org.keyczar.exceptions.KeyNotFoundException
                   (crypt/decrypt ks2 cipher1)))))
  (testing "defaulting to *crypter* works"
    (is (thrown? AssertionError (crypt/encrypt "foo")))
    (crypt/with-crypter (-> (keyset/create :crypt) (keyset/addkey))
      (is (= "foo" (crypt/decrypt (crypt/encrypt "foo")))))
    (is (thrown? AssertionError (crypt/encrypt "foo")))
    (try
      (crypt/set-crypter! (-> (keyset/create :crypt) (keyset/addkey)))
      (is (= "foo" (crypt/decrypt (crypt/encrypt "foo"))))
      (finally
        (alter-var-root #'crypt/*crypter* (constantly nil))))
    (is (thrown? AssertionError (crypt/encrypt "foo")))))
