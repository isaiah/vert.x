(ns vertx.verticle
  (:use (vertx core))
  (:import (org.vertx.java.core.buffer Buffer)))

(http-server "Server"
  (request-handler [req]
    {:data-handler (fn [^Buffer buf]
                     (println "buf" (.toString buf)))
     :body-handler (fn [^Buffer body]
                     (println (.toString body)))
     :end-handler (fn [& args]
                    (println "request end"))}
    (-> req .response (.end "macro test"))))
