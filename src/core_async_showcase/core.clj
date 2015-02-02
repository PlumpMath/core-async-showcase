(ns core-async-showcase.core
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
;;fun started here
;;;;;;;;;;;;;;;;;;;;

(put! c "hello world" (fn [] (println "done putting")))

(take! c #(println "GOT" %1))
