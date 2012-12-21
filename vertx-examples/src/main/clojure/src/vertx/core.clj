(ns vertx.core
  (:import (org.vertx.java.core Vertx Handler SimpleHandler AsyncResultHandler)
           (org.vertx.java.core.http HttpServerRequest RouteMatcher)
           (org.vertx.java.core.streams Pump)
           (clojure.lang ArityException))
  (:require [clojure.string :as s])
  (:use [clojure.java.shell :only [sh with-sh-dir]]))

(defn verticlize [x]
  (s/join (map s/capitalize (s/split x #"-"))))

(defn headers
  "Get the headers of the request"
  [^HttpServerRequest req]
  (.headers req))

(defn vertx-run [verticle]
  (println verticle))

(defmacro http-listen [port host & body]
  `(fn [vertx# _#]
     (let [http-server# (.createHttpServer vertx#)]
       ((fn [~'vertx ~'http-server] ~@body) vertx# http-server#)
       (.listen http-server# ~port ~host))))

(defmacro http-connect [port host & body]
  `(fn [vertx# _#]
     (let [http-client# (.createHttpClient vertx#)]
       (doto http-client# (.setPort ~port) (.setHost ~host))
       ((fn [~'vertx ~'client] ~@body) vertx# http-client#))))

(defmacro run-verticles [& verts]
  `(fn [_# container#]
     (doseq [v# (list ~@verts)]
       (doseq [name# (vals (ns-interns v#))]
         (when-let [verticle# (:verticle (meta name#))]
           (.deployVerticle container# verticle#))))))

(defmacro defverticle
  "Define a vertx verticle instance."
  [vert body]
  (let [this (gensym "this")
        prefix (gensym "prefix-")
        vert-class (-> vert str verticlize)]
    `(do
       (defn ~(vary-meta (symbol (str prefix "start")) assoc :verticle vert-class) [~this]
         (let [vertx# (.getVertx ~this)
               container# (.getContainer ~this)]
           (~body vertx# container#)))
       (gen-class
        :name ~vert-class
        :extends org.vertx.java.deploy.Verticle
        :prefix ~(str prefix)))))

(defmacro handler [expr & body]
  `(proxy [Handler] []
    (handle ~expr
      ~@body)))

(defmacro ws-handler [http-server expr & body]
  `(.websocketHandler ~http-server
                     (handler ~expr ~@body)))

(defmacro req-handler [http-server expr & body]
  `(.requestHandler ~http-server
                        (handler ~expr ~@body)))

(defmacro async-result-handler [expr & body]
  `(proxy [AsyncResultHandler] []
    (handle ~expr
      ~@body)))

(defmacro deploy-verticles [& args]
  `(defverticle ~(gensym "container")
     (run-verticles ~@args)))

(defmacro sock-connect [port host body]
  `(fn [vertx# container#]
     (let [client# (.createNetClient vertx#)]
       (.connect client# ~port ~host ~body))))

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

(defmacro data-handler [sock expr & body]
  `(.dataHandler ~sock
                 (handler ~expr ~@body)))

(defmacro sock-listen
  "create a net server, takes port host and connect handler"
  [port host & body]
  `(fn [vertx# container#]
     (let [server# (.createNetServer vertx#)]
       (.connectHandler server#
                        (proxy [Handler] []
                          (handle [~'sock]
                            ~@body)))
       (.listen server# ~port ~host))))

(defmacro http-route
  "Sinatra like route matching"
  [port host & body]
  `(fn [vertx# container#]
     (let [http# (.createHttpServer vertx#)
           router# (RouteMatcher.)]
       ((fn [~'router] ~@body) router#)
       (-> http#
           (.requestHandler router#)
           (.listen ~port ~host)))))

(defn open-file [vertx filename handler]
  (-> vertx .fileSystem
      (.open filename handler)))

(defn end-handler
  "call the callback when the request ends"
  [req callback]
  (.endHandler req callback))

(defn close-file [f callback]
  (.close f callback))

(defmacro get-now [client path & body]
  "Send a get request and block before the body returned"
  `(.getNow ~client ~path
            (proxy [Handler] []
              (handle [resp#]
                (.bodyHandler resp#
                              (proxy [Handler] []
                                (handle [data#]
                                  ((fn [~'buf] ~@body) data#))))))))

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

(defn pump
  ([sock1 sock2]
     (Pump/createPump sock1 sock2))
  ([sock1 sock2 start]
     (if start
       (.start (pump sock1 sock2))
       (pump sock1 sock2))))
