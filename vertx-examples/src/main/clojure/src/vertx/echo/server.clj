(ns vertx.echo.server
  (:use (vertx core)))

(net-server "Echo" {:port 1234}
  (connect-handler [sock]
    (pump sock sock)))
