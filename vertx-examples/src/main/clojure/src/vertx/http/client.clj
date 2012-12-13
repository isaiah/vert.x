(ns vertx.http.client
  (:use (vertx [core :as c])))

(c/http-connect 8080 "localhost"
  (fn [client]
    (c/get-now client "/"
      (fn [buf]
        (println buf)))))
