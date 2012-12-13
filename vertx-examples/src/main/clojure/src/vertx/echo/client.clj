(ns vertx.echo.client
  (:use (vertx [core :as c]))
  (:import (org.vertx.java.core.buffer Buffer)))

(c/connect 1234 "localhost"
  (fn [sock]
    (c/data-handler sock
      (fn [buf]
        (println "net client receiving:" buf)))
    (doseq [i (range 10)]
      (let [s (str "hello" i "\n")]
        (println "net client sending:" s)
        (->> s (Buffer.) (.write sock))))))