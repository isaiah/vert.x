(ns vertx.matcher
  (:use [clojure.core.match :only [match]]))

(defn -main [& args]
  (loop [a (range 10)]
    (match [a]
      [([x y & z] :seq)] (println x y) (recur z))))
