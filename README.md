# Kotoba package registry IPFS adapter

Verified JVM HTTP/IPFS retrieval for the pure package-registry contract owned by
`kotoba-lang/kotoba-lang`.

The adapter validates the requested CID before I/O, verifies the returned bytes
against that CID, decodes EDN, then delegates all registry validation and lock
construction to `kotoba.lang.package-registry`. IPFS is therefore a replaceable
transport, not a source of language or package semantics.

```sh
clojure -M:test
clojure -M:lint
```
