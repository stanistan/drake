(ns drake.drake-file.path
  (:require [clojure.string :as string]))

(def paths-to-ignore
  #{"_logs"})

(defn ignore-path?
  [file-path]
  (-> file-path
      (string/split #"/")
      last
      paths-to-ignore))

(defprotocol DrakePath
  (ignore? [this])
  (get-prefix [this])
  (get-filename [this]))

(defrecord Path [prefix filename]
  DrakePath
  (ignore? [this]
    (ignore-path? (get-filename this)))
  (get-prefix [this]
    (:prefix this))
  (get-filename [this]
    (:filename this)))

(defn str->DrakePath
  [file-path]
  (let [split (string/split file-path #":" -1)
        has-prefix? (> (count split) 1)]
    (->Path
      (if has-prefix? (keyword (first split)) :file)
      (if has-prefix? (string/join ":" (rest split)) (first split)))))
