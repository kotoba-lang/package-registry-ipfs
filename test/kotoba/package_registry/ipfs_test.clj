(ns kotoba.package-registry.ipfs-test
  (:require [clojure.test :refer [deftest is]]
            [kotoba.package-registry.ipfs :as ipfs]
            [multiformats.core :as mf]))

(defn cid [value]
  (mf/cidv1-raw (.getBytes (str value) "UTF-8")))

(def registry
  {:kotoba.registry/version 1
   :records
   [{:registry/name "kotoba/example"
     :registry/version "1.2.3"
     :registry/repo-rid (cid "repo")
     :registry/commit "0123456789abcdef"
     :registry/tree-cid (cid "tree")
     :registry/manifest-cid (cid "manifest")
     :registry/signers ["did:key:publisher"]
     :registry/capabilities []}]})

(deftest verified-bytes-enter-the-pure-registry
  (let [bytes (.getBytes (pr-str registry) "UTF-8")
        registry-cid (mf/cidv1-raw bytes)
        result (ipfs/lock-from-requests
                registry-cid
                [{:name "kotoba/example" :version "1.2.3"}]
                {:fetch-fn (fn [_ _] {:status 200 :bytes bytes})})]
    (is (:ok? result))
    (is (= registry-cid (mf/cidv1-raw bytes)))
    (is (= "0123456789abcdef" (get-in result [:deps 0 :dep/commit])))))

(deftest transport-fails-closed
  (let [bytes (.getBytes (pr-str registry) "UTF-8")
        requested (cid "different")]
    (is (= :registry/cid-mismatch
           (get-in (ipfs/lock-from-requests
                    requested []
                    {:fetch-fn (fn [_ _] {:status 200 :bytes bytes})})
                   [:problems 0 :problem])))
    (is (= :registry/fetch-http-status
           (get-in (ipfs/lock-from-requests
                    requested []
                    {:fetch-fn (fn [_ _] {:status 503 :bytes (byte-array 0)})})
                   [:problems 0 :problem])))))
