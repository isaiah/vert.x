(ns vertx.core
  (:import (org.vertx.java.core Vertx Handler SimpleHandler AsyncResultHandler)
           (org.vertx.java.core.http HttpServerRequest RouteMatcher)
           (org.vertx.java.core.streams Pump)
           (clojure.lang ArityException)))

(defn headers
  "Get the headers of the request"
  [^HttpServerRequest req]
  (.headers req))

(defmacro defverticle
  "Define a vertx verticle instance."
  [name & body]
  (let [this (gensym "this")
        prefix (gensym "prefix")]
    `(do
       (defn ~(symbol (str prefix "start")) [~this]
         (let [vertx# (.getVertx ~this)]
           (~@body vertx#)))
       (gen-class
         :name ~name
         :extends org.vertx.java.deploy.Verticle
         :prefix ~prefix))))

(defmacro connect [port host & body]
  (let [this (gensym "this")
        prefix (gensym "-")]
    `(do
       (defn ~(symbol (str ~prefix "start")) [~this]
         (let [vertx# (.getVertx ~this)
               client# (.createNetClient vertx#)]
           (.connect client# ~port ~host
             (proxy [Handler] []
               (~'handle [~'sock]
                 (~@body ~'sock))))))
       (gen-class
         :name "Client"
         :extends org.vertx.java.deploy.Verticle
         :prefix ~prefix))))

(defn data-handler [sock data-fn]
  (.dataHandler sock
    (proxy [Handler] []
      (handle [buf]
        (data-fn buf)))))

(defmacro net-server [name config & body]
  (let [{:keys [host port] :or {host "localhost" port 8080}} config
        this (gensym "this")
        prefix (gensym "-")]
    `(do
       (defn ~(symbol (str ~prefix "start")) [~this]
         (let [vertx# (.getVertx ~this)
               net# (.createNetServer vertx#)]
           (.connectHandler net# ~@body)
           (.listen net# ~port ~host)))
       (gen-class
         :name ~name
         :extends org.vertx.java.deploy.Verticle
         :prefix ~prefix))))

(defmacro connect-handler [sock & body]
  `(proxy [Handler] []
     (~'handle ~sock
       ~@body)))

;(defn connect-handler [connect-fn]
;  (proxy [Handler] []
;    (handle [sock]
;      (connect-fn sock))))

(defn http-listen [port host req-fn]
  (fn [vertx]
    (let [http# (.createHttpServer vertx)]
      (.requestHandler http#
               (proxy [Handler] []
                 (handle [req]
                   (req-fn req))))
      (.listen http# port host))))

(defmacro http-route
  "Sinatra like route matching"
  [port host routes]
  (let [this (gensym "this")
        prefix (gensym "-")]
    `(do
       (defn ~(symbol (str ~prefix "start")) [~this]
         (let [vertx# (.getVertx ~this)
               http# (.createHttpServer vertx#)
               router# (RouteMatcher.)]
           (~routes router#)
           (-> http#
             (.requestHandler router#)
             (.listen ~port ~host))))
       (gen-class
         :name "Sinatra" ; place holder
         :extends org.vertx.java.deploy.Verticle
         :prefix ~prefix))))

(defmacro http-connect [port host & body]
  (let [this (gensym "this")
        prefix (gensym "-")]
    `(do
       (defn ~(symbol (str ~prefix "start")) [~this]
         (let [vertx# (.getVertx ~this)
               http# (.createHttpClient vertx#)]
           (doto http# (.setPort ~port) (.setHost ~host))
           (~@body http#)))
       (gen-class
         :name "HttpClient"
         :extends org.vertx.java.deploy.Verticle
         :prefix ~prefix))))

(defn open-file [vertx filename file-fn]
  (-> vertx .fileSystem
    (.open filename
      (proxy [AsyncResultHandler] []
        (handle [ar]
          (file-fn (.result ar)))))))

(defn end-handler
  "call the callback when the request ends"
  [req callback]
  (.endHandler req
    (proxy [SimpleHandler] []
      ; this method is called with nil.
      (handle [_]
        (callback)))))

(defn close-file [f callback]
  (.close f
    (proxy [AsyncResultHandler] []
      (handle [ar]
        (if (nil? (.exception ar))
          (callback ar)
          (-> ar .exception (.printStackTrace (System/err))))))))

(defn get-now [client path body-fn]
  "Send a get request and block before the body returned"
  (.getNow client path
    (proxy [Handler] []
      (handle [resp]
        (.bodyHandler resp
          (proxy [Handler] []
            (handle [data]
              (body-fn data))))))))

(defn http-put [client ])
(defn end-req
  "syntax sugar for end request"
  ([req buf]
    (.end (.response req) buf))
  ([req]
    (.end (.response req))))

(defn send-file [req file]
  "syntax sugar for sendFile"
  (.sendFile (.response req) file))

(defn params [req key]
  "syntax sugar for get parameters"
  (-> req .params (.get key)))

;(defmacro request-handler [req handlers & body]
;  (let [{:keys [data-handler body-handler end-handler]} handlers]
;    `(proxy [Handler] []
;       (~'handle [^HttpServerRequest ~@req]
;         (if (not (nil? ~data-handler))
;           (.dataHandler ~@req (proxy [Handler] []
;                                 (~'handle [~'buf]
;                                   (~@data-handler ~'buf)))))
;         (if (not (nil? ~body-handler))
;           (.bodyHandler ~@req (proxy [Handler] []
;                                 (~'handle [~'body-buffer]
;                                   (~@body-handler ~'body-buffer)))))
;         ;(if (not (nil? ~end-handler))
;         ;  (.endHandler ~@req (proxy [SimpleHandler] []
;         ;                       (~'handle []
;         ;                         (~@end-handler)))))
;         ~@body))))

(defn pump
  ([sock1 sock2]
    (Pump/createPump sock1 sock2))
  ([sock1 sock2 start]
    (if start
      (.start (pump sock1 sock2))
      (pump sock1 sock2))))
