(defproject circleci/clj-keyczar "0.1.1"
  :description "small, simple clojure wrapper for keyczar"
  :url "http://github.com/circleci/clj-keyczar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.4"]
                 ;; This is because there's no official keyczar release in maven central :(
                 [circleci/keyczar "0.71g"]])
