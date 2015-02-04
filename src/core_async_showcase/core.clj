(ns core-async-showcase.core
  (:refer-clojure :exclude [map reduce into partition partition-by take merge])
  (:require [clojure.core.async :refer :all :as async]
            [clojure.pprint :refer [pprint]]
            ))
  
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

;;(pprint (macroexpand '(go 42)))


;; buffer chan

(def fixed-length-buffer-can (chan 1))

(go (>! fixed-length-buffer-can 1)
    (println "DONE 1"))

(go (>! fixed-length-buffer-can 2)
    (println "DONE 2"))

(<!! fixed-length-buffer-can)
(<!! fixed-length-buffer-can)

;; dropping buffer (drop newest)

(def dropping-buffer-channel (chan (dropping-buffer 1)))

(go (>! dropping-buffer-channel 1)
    (println "DONE dropping-1"))

(go (>! dropping-buffer-channel 2)
    (println "DONE dropping-2")
    )

(<!! dropping-buffer-channel)


;;; sliding buffer (e.g TCP sliding window like buffer)

(def sliding-buffer-channel (chan (sliding-buffer 1)))

(go (>! sliding-buffer-channel 1)
    (println "DONE sliding-1"))

(go (>! sliding-buffer-channel 2)
    (println "DONE sliding-2"))

(<!! sliding-buffer-channel)


;;; close channel

(def c (chan))
(go (>! c 1))
;;; canT do this outside go block!!!!!!
;(>! c 1)
(<!! c)

(close! c)
(go (>! c 1))
(<!! c)


;;; ALT % TIMEOUT

;;; usage : takes 1st item out of the can

(def can-a (chan))
(def can-b (chan))

(put! can-a 42)
(put! can-b 99)

(alts!! [can-a can-b])

;;;note the usage in real world will be shown later


;;; close a channel if nothing recv in X ms

(<!! (timeout 5000))

;(def timeout-can-a (timeout 1000))
;(def normal-chan (chan))
;(alts!! [timeout-can-a normal-chan])


;;; put with alt!

;TODO

;;; alt with default
        

(alts!! [can-a can-b] :default "nothing in here, close!")
;;; 
;;; diference in ! and !!

;;; TODO 1.difference between ! and !!
;;;      2.differnece between alt! alt!! and alts! alts!!
;;;      3.benifit of channel/alts approach over queue or a.k.a java BlockingQueue




