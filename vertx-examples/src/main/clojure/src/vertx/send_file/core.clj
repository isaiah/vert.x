(ns vertx.send_file.core
  (:use (vertx [core :as c])))

(def webroot "/home/isaiah/codes/java/vert.x/vertx-examples/src/main/clojure/src/vertx/send_file/")
(c/http-listen 8080 "localhost"
  (fn [req]
    (let [path (.path req)]
      (if (= path "/")
        (c/send-file req (str webroot "index.html"))
        (c/send-file req (str webroot path))))))
