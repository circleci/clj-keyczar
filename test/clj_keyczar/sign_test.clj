(ns clj-keyczar.sign-test
  (:require [clojure.test :refer :all]
            [clj-keyczar.keyset :as keyset]
            [clj-keyczar.sign :as sign]))

(deftest ->signer-works
  (testing "attempts to coerce a path into a signer"
    (is (thrown-with-msg? org.keyczar.exceptions.KeyczarException #"Error reading file"
                          (sign/->signer "/blahbitty-nonexistent"))))
  (let [keyset (keyset/create :sign)]
    (testing "coerces a keyset into a signer"
      (is (isa? (type (sign/->signer keyset)) org.keyczar.Signer)))
    (testing "coerces a KeyczarReader into a signer"
      (is (isa? (type (sign/->signer (keyset/->KeyczarReader keyset))) org.keyczar.Signer)))
    (let [signer (sign/->signer keyset)]
      (testing "noops things that are already signers"
        (is (= signer (sign/->signer signer))))))
  (testing "otherwise throws"
    (is (thrown? IllegalArgumentException (sign/->signer 9)))))

(deftest signing-and-verifying-work
  (testing "happy path works"
    (let [keyset (-> (keyset/create :sign)
                     (keyset/addkey))
          plain "foo"
          signature (sign/sign keyset plain)]
      (is (sign/verify keyset plain signature))))
  (testing "different signers are incompatible"
    (let [ks1 (-> (keyset/create :sign)
                  (keyset/addkey))
          ks2 (-> (keyset/create :sign)
                  (keyset/addkey))
          plain "foo"
          signature1 (sign/sign ks1 plain)
          signature2 (sign/sign ks2 plain)]
      (is (not= signature1 signature2))
      (is (thrown? org.keyczar.exceptions.KeyNotFoundException
                   (sign/verify ks1 plain signature2)))
      (is (thrown? org.keyczar.exceptions.KeyNotFoundException
                   (sign/verify ks2 plain signature1)))))
  (testing "verifying the wrong message XXX"
    (let [keyset (-> (keyset/create :sign)
                     (keyset/addkey))
          plain1 "foo"
          plain2 "bar"]
      (is (not (sign/verify keyset plain1 (sign/sign keyset plain2))))
      (is (not (sign/verify keyset plain2 (sign/sign keyset plain1))))))
  (testing "defaulting to *signer* works"
    (is (thrown? AssertionError (sign/sign "foo")))
    (sign/with-signer (-> (keyset/create :sign) (keyset/addkey))
      (is (sign/verify "foo" (sign/sign "foo"))))
    (is (thrown? AssertionError (sign/sign "foo")))
    (try
      (sign/set-signer! (-> (keyset/create :sign) (keyset/addkey)))
      (is (sign/verify "foo" (sign/sign "foo")))
      (finally
        (alter-var-root #'sign/*signer* (constantly nil))))
    (is (thrown? AssertionError (sign/sign "foo")))))
