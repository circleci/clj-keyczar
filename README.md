# clj-keyczar

A somewhat opinionated clojure wrapper around the [Keyczar](http://www.keyczar.org/) library.

This doesn't expose the full set of Keyczar functionality! It basically uses the (very
sensible) defaults for everything, and supports just the following:

* symmetric encryption and decryption
* signing and verifying messages
* managing keysets as clojure maps: adding, demoting and revoking keys

## Usage

Add to your project.clj:

```clojure
    :dependencies [[circleci/clj-keyczar "0.1.0"]]
```

# crypt/decrypt

Create a new crypting keyset, add a key to it, encrypt and decrypt data:

```clojure
(let [keyset (-> (keyset/create :crypt)
                 (keyset/addkey))
      original "foo"
      ciphertext (crypt/encrypt keyset original)
      decrypted (crypt/decrypt keyset ciphertext)]
  (assert (= original decrypted)))
```

# sign/verify

Create a new signing keyset, add a key to it, sign and verify messages:

```clojure
(let [keyset (-> (keyset/create :sign)
                 (keyset/addkey))
      original "foo"
      signature (sign/sign keyset original)]
  (assert (sign/verify keyset original signature)))
```

# keyset operations

The support for key versioning and rotation is really what makes Keyczar awesome, and it's
frankly better documented better in
[the Keyczar documentation](https://code.google.com/p/keyczar/). In a nutshell, this wrapper
supports:

* creating new keysets, of purpose :crypt or :sign.
* adding a key to a keyset. The newly added key will be primary and active.
* demoting a key from a keyset.
* revoking a key from a keyset.

This library treats keysets as immutable; functions return brand-new keysets with the
requested changes applied. You will almost certainly need a separate mechanism for persisting
keysets. On the plus side, they're just trivially serializable maps. (For what it's worth,
we use the same system for storing keysets that we use to e.g. propagate database credentals
through our system.)

## Some crypto details

Though the keyczar library has a few bells and whistles, it provides great defaults -- so
great, in fact, that they're all this wrapper currently supports. Those defaults are:

* for :crypt keysets: 128-bit AES keys
* for :sign keysets: 256-bit HMAC keys

Other stuff currently not supported:

* asymmetric encryption
* file-based keysets (the crypt and sign nses work with them, but the keyset ns does not; use [KeyczarTool](https://code.google.com/p/keyczar/wiki/KeyczarTool) to manage these keysets!)
* pretty much anything that's not on the straight and narrow path of default keyczar behavior :P

Feel free to open issues or submit PRs to improve the library :)

## License

Copyright © 2014 CircleCI

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
