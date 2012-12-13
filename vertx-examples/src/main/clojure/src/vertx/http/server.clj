(ns vertx.http.server
  (:use (vertx [core :as c])))

(c/defverticle "HttpServer"
  (c/http-listen 8080 "localhost"
    (fn [req]
      (println "got request:" (.uri req))
      (println "headers:")
      (doseq [[k v] (headers req)]
        (println k ":" v))
      (-> req .response .headers (.put "Content-Type" "text/html; charset=UTF-8"))
      (-> req .response (.end "<html><body><h1>Hello from vertx!</h1></body></html>")))))