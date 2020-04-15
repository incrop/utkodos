(ns utkodos
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]))

(import 'javax.sound.sampled.AudioSystem)
(import 'java.util.concurrent.TimeUnit)

(defn meow []
  (let [clip (AudioSystem/getClip)]
    (.open clip (AudioSystem/getAudioInputStream (io/input-stream "./meow.wav")))
    (.start clip)))

(defn fetch-earliest [address-id]
  (try
    (let [timestamp (quot (System/currentTimeMillis) 1000)
          utk-url (str "https://www.utkonos.ru/utkax.php/intervals/response?address_id=" address-id "&_=" timestamp)
          resp-raw (slurp utk-url)
          resp (json/read-str resp-raw)]
      (spit "./last-response.json" resp-raw)
      (->>
        (get-in resp ["ajax" "intervals" "items"])
        (filter #(not (get % "disabled")))
        (map #(get % "delivery_date"))
        (sort)
        (first)))
    (catch Exception e (println "ouch!" e))))

(defn -main [address-id]
  (println "Sound check!")
  (meow)
  (loop [prev "9999-01-01 00:00:00"]
    (.sleep TimeUnit/SECONDS 30)
    (let [curr (fetch-earliest address-id)]
      (cond
        (nil? curr) (do
          (println "No intervals")
          (recur prev))
        (<= 0 (compare curr prev)) (do
          (println "Earliest available:" curr)
          (recur prev))
        :else (do
          (println "Meow!" curr)
          (meow)
          (recur curr))))))
