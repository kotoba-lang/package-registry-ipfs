(ns kotoba.package-registry.ipfs
  "Verified JVM HTTP/IPFS transport for the pure Kotoba package registry."
  (:require [clojure.edn :as edn]
            [kotoba.lang.package-contract :as contract]
            [kotoba.lang.package-registry :as registry]
            [multiformats.core :as mf])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers]
           [java.time Duration]))

(defn- http-fetch
  [cid {:keys [gateway-base timeout-ms]}]
  (let [request (-> (HttpRequest/newBuilder)
                    (.uri (URI/create (str gateway-base cid)))
                    (.timeout (Duration/ofMillis timeout-ms))
                    (.GET)
                    (.build))
        response (.send (HttpClient/newHttpClient) request
                        (HttpResponse$BodyHandlers/ofByteArray))]
    {:status (.statusCode response) :bytes (.body response)}))

(defn lock-from-requests
  "Fetch CID, verify exact bytes, decode EDN, then delegate resolution to the
  pure language registry. `fetch-fn` is injectable for alternate transports and
  deterministic tests; it returns {:status integer :bytes byte-array}."
  ([cid requests] (lock-from-requests cid requests {}))
  ([cid requests {:keys [gateway-base timeout-ms fetch-fn]
                  :or {gateway-base "http://127.0.0.1:8080/ipfs/"
                       timeout-ms 10000
                       fetch-fn http-fetch}}]
   (if-not (contract/cid? cid)
     {:ok? false :problems [{:problem :registry/cid-invalid}]}
     (try
       (let [{:keys [status bytes]}
             (fetch-fn cid {:gateway-base gateway-base
                            :timeout-ms timeout-ms})]
         (cond
           (not= 200 status)
           {:ok? false :problems [{:problem :registry/fetch-http-status
                                   :status status}]}
           (not= cid (mf/cidv1-raw bytes))
           {:ok? false :problems [{:problem :registry/cid-mismatch}]}
           :else
           (registry/lock-from-requests
            (edn/read-string (String. ^bytes bytes "UTF-8")) requests)))
       (catch Exception error
         {:ok? false
          :problems [{:problem :registry/fetch-failed
                      :message (ex-message error)}]})))))
