(ns vertx.upload.client
  (:require [vertx.core :as v]))

(v/http-connect 8080 "localhost"
  (fn [client]
    (v/http-put client "/"
      (fn [buf]
        (println buf)))))