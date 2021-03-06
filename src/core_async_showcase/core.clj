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

;;;go block, the key concept of core.async
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

;;;TODO lib upgraded this is not working anymore, find out how
;(def timeout-can-a (timeout 1000))
;(def normal-chan (chan))
;(alts!! [timeout-can-a normal-chan])

;;; default
(alts!! [can-a] :default :nothing-in-the-channel)
;;; NOTICE by default alts! and alts!! choose in random order

(put! can-a :a) ;eval this n times
(put! can-b :b) 

(alts!! [can-a can-b]) ;DISORDERED!!!

;;; priority
(put! can-a :a-priority)
(put! can-b :b-priority)

(alts!! [can-a can-b])

;;; log

(def logging-chan (chan))

(thread
 (loop []
   (when-let [v (<!! logging-chan)]
     (println v)
     (recur)
     )
   )
 (println "end logging")
 )

(defn log [msg]
  (>!! logging-chan msg)
  )

(log "hehe")


;;; Thread pool
(defn thread-pool-service [c func max-threads timeout-ms]
  (let [thread-count (atom 0)
        buffers-status (atom 0)
        buffer-chan (chan)
        thread-fn (fn []
                    (swap! thread-count inc)
                    (loop []
                      (when-let [val (first (alts!! [buffer-chan (timeout timeout-ms)]))]
                        (func val)
                        (recur)))
                    (swap! thread-count dec)
                    (println "Exiting"))]

    (go (loop []
          (when-let [val (<! c)]
            (if-not (alt! [[buffer-chan val]] true :default false)
              (loop []
                (if (< @thread-count max-threads)
                  (do (put! buffer-chan val)
                      (thread (thread-fn)))
                  (when-not (alt! [[buffer-chan val]] true
                                  [(timeout 1000)]
                                  ([_] false))
                    (recur)))))
            (recur)))
        (close! buffer-chan))))

(def executing-chan (chan))


(def thread-pool (thread-pool-service executing-chan
                                      (fn [x] (println x)
                                        (Thread/sleep 5000))
                                      3
                                      3000
                                      ))

(>!! executing-chan "hello")


;; broadcasting
;;;TODO the 0.1.2 is not the same as 0.1.3 need confirm the expecting
;;behvaiousi????

;;; 1.def mutl-chan
;;; 2.tap to noraml chan

(def to-multi  (chan 1))
(def m (mult to-multi))

(let [c (chan 1)
      c-2 (chan 2)]
  (tap m c)
  (tap m c-2)
  (go (loop []
        ;;when c have sth
        (when-let [val (<! c)]
          (println "GOT sth from C" val))
        ;;then c-2 should also have sth
        (when-let [val (<! c-2)]
          (println "GOT sth from C-2" val)
          )
        (recur)
        )
      (println "EXITING")))


(>!! to-multi 1)
(>!! to-multi 2)

(close! to-multi)


;;; PUB/SUB
;;; multi version of multimethod

(def to-pub (chan 1))
(def p (pub to-pub :tag))

(def printing-chan (chan 1))

(go (loop []
      (when-let [val (<! printing-chan)]
        (println val)
        (recur))))

(let [c (chan 1)]
  (sub p :java c)
  (go (println "JAVA news:")
      (loop []
        (when-let [val (<! c)]
          (>! printing-chan (pr-str "JAVA nerd got news :" (:msg val)))))))

(let [c (chan 1)]
  (sub p :c c)
  (go (println "c news:")
      (loop []
        (when-let [val (<! c)]
          (>! printing-chan (pr-str "c nerd got news :" (:msg val)))))))

;;; DOWN SIDE no default impl, but ....lisper normally dont wrote this
;;; kind of shity code...
(let [c (chan 1)]
  (sub p :default c)
  (go (println "default news:")
      (loop []
        (when-let [val (<! c)]
          (>! printing-chan (pr-str "x nerd got news :" (:msg val)))))))

;; (defn regist-news-feed [{tag :tag
;;                          title :title
;;                          :as all}]
;;   (println "regist" tag)
;;   (let [c (chan 1)]
;;     (sub p tag c)
;;     (go (println title "news:")
;;         (loop []
;;           (when-let [val (<! c)]
;;             (>! printing-chan (pr-str title "Got news :" val))))))
;;   )


(defn send-with-tags [msg]
  (doseq [tag (:tags msg)]
    (println "sending..." tag)
    (>!! to-pub {:tag tag
                 :msg (:msg msg)})))


;; (regist-news-feed {:tag :C
;;                    :title "C programmer"})

(send-with-tags {:msg "JVM crashes everytime!"
                 :tags [:java]})
(send-with-tags {:msg "C core-dump methods "
                 :tags [:C]})
(send-with-tags {:msg "clj bla ba "
                 :tags [:clojure]})

(close! to-pub)


;;; ACTOR model

(defprotocol IActor
  (! [this msg]))

(defn spawn [f]
  (let [c (chan Integer/MAX_VALUE)]
    (go (loop [f f]
          (recur (f (<! c)))))
    (reify IActor
      (! [this msg]
        (put! c msg)))))

(defn spawn-counter []
  (let [counter (fn counter [cnt msg]
                  (case (:type msg)
                    :inc (partial counter (inc cnt))
                    :get (do (! (:to msg) cnt)
                             (partial counter cnt))))]
    (spawn (partial counter 0))))

(defn printer []
  (spawn (fn ptr [msg]
           (println "GOT > " msg)
           ptr)))

(def prt (printer))

(def counter (spawn-counter))

(! counter {:type :inc})



;;; put with alt!


;;; alt with default
        

(alts!! [can-a can-b] :default "nothing in here, close!")

;;; 
;;; diference in ! and !!

;;; TODO 1.difference between ! and !!
;;;      2.differnece between alt! alt!! and alts! alts!!
;;;      3.benifit of channel/alts approach over queue or a.k.a java
;;;      BlockingQueue
;;;      4.async testing


(def c (to-chan '(1 2 3)))


(defn add-1 [x]
  (+ x 1))

(def mapped-c (map add-1 [c]))
(<!! mapped-c)





