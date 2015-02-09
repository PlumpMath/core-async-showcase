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

(defn default-delay-behavious
  []
  (delay (let [msg "bla bla bla...."]
           (println "trying deref :" msg)
           msg))
  )
