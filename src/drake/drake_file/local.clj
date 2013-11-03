(ns drake.drake-file.local
  (:use drake.shell
        drake.drake-file.path
        drake.drake-file.core)
  (:require [fs.core :as fs]))

(defn- get-files-in-directory
  [dir-name]
  (map #(.getPath %) (.listFiles (fs/file dir-name))))

(defn- with-path
  [& fns]
  (let [composed (apply comp fns)]
    (fn [file]
      (composed (:path file)))))

(defn- with-file
  [& fns]
  (apply with-path (conj (into [] fns) fs/file)))

(defdrakefile LocalFile :file [path]

  get-path
  :path

  get-normalized-name
  (fn [this] (get-path this))

  exists?
  (with-file fs/exists?)

  directory?
  (with-file fs/directory?)

  mod-time
  (with-path fs/mod-time)

  get-seq
  (fn [file]
    (if (or (ignore-path? (get-path file)) (not (exists? file)))
      []
      (if (directory? file)
        (mapcat #(get-seq (->LocalFile %)) (get-files-in-directory (get-path file)))
        [file])))

  rm
  (fn [file]
    ;; TODO(artem)
    ;; This is dirty, we probably should re-implement this using syscalls
    (shell "rm" "-rf" (get-path file) :use-shell true :die true))

  mv
  (fn [file to]
    ;; TODO(artem)
    ;; This is dirty, we probably should re-implement this using syscalls
    (shell "mv" (get-path file) to :use-shell true :die true)))
