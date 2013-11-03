(ns drake.drake-file.utils
  (:require [clojure.string :as string]))

(defn map-keys
  [f hash-map]
  (into {} (map (fn [[k v]] [(f k) v]) hash-map)))

(defn class->chunks
  [class]
  (-> class
      str
      (string/replace "class " "")
      (string/replace "_" "-")
      (string/split #"\.")))

(defn chunks->fn-name
  [class-chunks]
  (let [prefix (string/join "." (butlast class-chunks))
        suffix (last class-chunks)]
    (str prefix "/->" suffix)))

(defn ->fn
  [class]
  (-> class class->chunks chunks->fn-name symbol eval))
