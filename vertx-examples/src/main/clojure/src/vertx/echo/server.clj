(ns vertx.echo.server
  (:use (vertx core)))

(defverticle echo-server
  (sock-listen 1234 "localhost"
               (pump sock sock true)))
