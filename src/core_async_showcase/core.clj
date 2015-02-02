(ns core-async-showcase.core
  (:refer-clojure :exclude [map reduce into partition partition-by take merge])
  (:require [clojure.core.async :refer :all :as async]) 
  )
;;; channel with default parameters
(def c (chan))

(chan)

(take! c (fn [v] (println v)))

(put! c 42)

(put! c "hehe")

(take! c (fn [v] (println "GOT" v)))

;;;;;;;;;;;;;;;;;;;;;
;;call-back like approach
;;;;;;;;;;;;;;;;;;;;

(put! c "hello world" (fn [x] (println  "DONE PUTTING")))

(take! c #(println "GOT" %1))

;;; 
;;; <!! and >!!
;;;

(defn take-item [c]
  (let [product (promise)]
    (take! c (fn [v] (deliver product v)))
    @product
    )
  )

(future (println "GOT" (take-item c)))

(put! c 42)

;;reverse using put
(defn put-item [c val]
  (let [product (promise)]
    (put! c val (fn [x] (deliver product nil)))
    @product
    )
  )

(future (println "DONE" (put-item c 42)))

(future (println "GOT" (take-item c)))

;;; using >!! and <!! , the same ALMOST
(future (println "DONE (>!!)" (>!! c 42)))
(future (println "GOT (<!!)" (<!! c )))

;;future returns a promise, more or less like java Future
;;but <!! returns a channel, which is much much much more flexiable
;;and powerfull
(thread 42)

(<!! (thread 42))

(thread (println "async version return channel." (<!! (thread 42))))

;;;其他都是空了吹，以下才是真家伙 go 真的是go
(go 42)

(<!! (go 42))

(go (println "go also works!" (<! (go 42))))


























