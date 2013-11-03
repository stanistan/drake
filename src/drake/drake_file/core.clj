(ns drake.drake-file.core
  (:use drake.drake-file.path
        drake.drake-file.utils)
  (:require [clojure.string :as string]))

;; DrakeFile base (shared/default) implementation ........................................
;; .......................................................................................

(def drakefile-default-implementations {})

;; Dispatch on type ......................................................................
;; .......................................................................................

(defprotocol DrakeFile
  (get-path [file])
  (get-normalized-name [file])
  (exists? [file])
  (directory? [file])
  (mod-time [file])
  (get-seq [file])
  (rm [file])
  (mv [file to]))

(defmulti get-drakefile-classname
  "Dispath on the prefix of the DrakePath, which is a keyword."
  (fn [drake-path] (get-prefix drake-path)))

(defmacro defdrakefile
  "Creates a DrakeFile record given a name and implemtation.
   Registers the method for dispatching in get-drakefile-classname.

   (defdrakefile FileName :dispatch-key [path and other things]
    ...)"
  [record-name dispatch-keyword record-props & implementation-pairs]
  (let [implementation (->> implementation-pairs
                            (apply hash-map)
                            (map-keys keyword)
                            (merge drakefile-default-implementations))]
    `(do
      (defrecord ~record-name ~record-props)
      (defmethod get-drakefile-classname ~dispatch-keyword [_#] ~record-name)
      (extend ~record-name DrakeFile ~implementation))))

(defn DrakePath->DrakeFile
  [drake-path & args]
  (apply (-> drake-path get-drakefile-classname ->fn)
         (get-filename drake-path)
         args))

(defn path->DrakeFile
  [path & args]
  (apply DrakePath->DrakeFile (str->DrakePath path) args))

(defn apply-to-path
  [f path & args]
  (apply f (path->DrakeFile path) args))
