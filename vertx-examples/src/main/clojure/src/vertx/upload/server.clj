(ns vertx.upload.server
  (:use (vertx [core :as v])))

(def webroot "/home/isaiah/codes/java/vert.x/vertx-examples/src/main/clojure/src/vertx/upload/")

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(v/http-listen 8080 "localhost"
  (fn [req vertx]
    ; we first pause the request so we don't receive any data
    ; between now and when the file is opened
    (.pause req)
    (let [filename (str webroot "file-" (uuid) ".upload")]
      (v/open-file vertx filename
        (fn [file]
          (let [p (v/pump req (.getWriteStream file))
                start (System/currentTimeMillis)]
            (v/end-handler req
              (fn []
                (v/close-file file
                  (fn [_]
                    (v/end-req req)
                    (let [end (System/currentTimeMillis)]
                      (println "uploaded" (.getBytesPumped p)
                        "byte to" filename "in" (- end start) "ms"))))))
            (.start p)
            (.resume req)))))))
