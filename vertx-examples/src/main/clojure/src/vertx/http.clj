(ns vertx.http
  (:import (org.vertx.java.core Vertx Handler)
           (org.vertx.java.core.http HttpServerRequest))
  (:gen-class
    :name Verticle
    :extends org.vertx.java.deploy.Verticle
    :prefix "-"))

(defn -start [this]
  (let [server (-> this .getVertx .createHttpServer)]
    (.requestHandler server (proxy [Handler] []
                              (handle [^HttpServerRequest req]
                                (-> req .response (.end "hello world")))))
    (.listen server 8080)))
