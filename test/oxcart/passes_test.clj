;;   Copyright (c) Reid McKenzie, Rich Hickey & contributors. The use
;;   and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0
;;   (http://opensource.org/licenses/eclipse-1.0.php) which can be
;;   found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be
;;   bound by the terms of this license.  You must not remove this
;;   notice, or any other, from this software.

(ns oxcart.passes-test
  (:require [oxcart.passes :refer :all]
            [clojure.test :refer :all]))


(deftest whole-ast->modules-tests
  (is (= [{:foo :bar}]
         (-> {:modules [:baz]
              :baz {:foo :bar}}
             whole-ast->modules
             vec))))


(deftest whole-ast->forms-tests
  (is (= [{:foo :bar}
          {:foo :quxx}]
         (-> {:modules [:baz]
              :baz {:forms [{:foo :bar}
                            {:foo :quxx}]}}
             whole-ast->forms
             vec))))


(deftest update-forms-tests
  (is (= {:modules [:baz]
          :baz {:forms [2]}}
         (-> {:modules [:baz]
              :baz {:forms [1]}}
             (update-forms inc)))))


(defn pass-one [ast {:keys [log] :as options}]
  (when log
    (swap! log conj pass-one))

  (-> ast
      (update-forms inc)
      (record-pass pass-one)))


(deftest record-pass-tests
  (is (= #{pass-one}
         (-> {:modules [:baz]
              :baz {:forms [1]}}
             (pass-one {})
             :passes))))


(defn pass-two [ast {:keys [log] :as options}]
  (when log
    (swap! log conj pass-two))

  (-> ast
      (require-pass pass-one options)
      (update-forms dec)
      (record-pass pass-two)))


(deftest require-pass-tests
  (is (= #{pass-one
           pass-two}
         (-> {:modules [:baz]
              :baz {:forms [1]}}
             (pass-two {})
             :passes))))


(deftest do-passes-tests
  (is (let [log (atom [])]
        (= [pass-two pass-one]
           (do (-> {:modules [:baz]
                    :baz {:forms [1]}}
                   (do-passes {:log log}
                              pass-two
                              pass-one))
               (vec @log))))))


(deftest clobber-passes-tests
  (is (= #{}
         (-> {:modules [:baz]
              :baz {:forms [1]}}
             (do-passes {}
                        pass-two
                        pass-one)
             (clobber-passes)
             :passes))))
