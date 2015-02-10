(ns core-async-showcase.default
  (:require [clojure.pprint :refer [pprint]]))

(defn future-will-print-later
  []
  (future (Thread/sleep 3000)
          (println "inside future, print in 3 secs later."))
  (println "outside future, println instantly.")
  )

;;; with out thread, default behavious of future NOTICE need deref to
;;; get value

(defn default-behavious-of-future
  []
  (let [result (future (println "inside block, print once")
                       (+ 1 1))]
    (println "deref" (deref result))
    (println "@:" @result)))

;;; deref future blocks future (thread or not)

(defn deref-unfinished-future-will-block
  []
  (println "Instantly run...\n")
  (let [result (future  (Thread/sleep 3000)
                        (+ 1 1))
        ]
    (println "Result is :" @result)
    (println "But in 3 secs later...")
    )
  )

;;; limit the waiting/blocking time

(deref (future  (Thread/sleep 5000) 0)
       1000 5)

;;; safe ? or more c like graduian like programming

(realized? (future (Thread/sleep 1000)))

(let [f (future)]
  @f
  (realized? f)
  )

;;; 
;;; deref  a.k.a suger @
;;; seprating 1. task definition 2.task excution 3.result collecting
;;;


;;; Delays

(def delay-val (delay (println "eval once, then cache") (+ 1 1)))

delay-val

(force delay-val)

@delay-val

;;; use case
;;; post notification, when X is done, trigger some Y event

(def items ["xx.avi"
            "tokyo-hot.rmvb"
            "playbody.epub"])

(defn notiy-down-comletion
  [user-id]
  (println "Hello, item -> " user-id  "is downloaded" ))

(defn download-item
  [item]
  (println "playing -> " item) 
  )

(let [notify (delay (notiy-down-comletion "me"))]
  (doseq [item items]
    (future (view-item item)
            (force notify))))

;;; Promises

(def my-promise (promise))

(deliver my-promise (+ 1 1))

@my-promise

;;; NOTICE derefing a promise will cause BLOCK

(let [some-func-in-js-promise (promise)]
  (future (println "Here's some js fn: " @some-func-in-js-promise)
          )
  (Thread/sleep 1000)
  (deliver some-func-in-js-promise "function blabla"))


