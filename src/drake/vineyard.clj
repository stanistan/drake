(ns drake.vineyard
  (:require [fs.core :as fs])
  (:use [clojure.tools.logging :only [info debug]]
        [slingshot.slingshot :only [throw+]])
  (:import [vineyard Task TaskQueue]))

(def END-STATUS #{vineyard.Task$Status/DONE
                  vineyard.Task$Status/ERROR
                  vineyard.Task$Status/CANCELED})

(defn fingerprint
  "Returns a unique fingerprint derived from the vineyard-specific values of the step."
  [conf task]
  (comment (.hashCode (merge conf task)))
  (str (java.util.UUID/randomUUID))) ; NOTE(Myron) for now, make it completely random because I was getting collisions

(defn task-file [conf task]
  (fs/file (str (fingerprint conf task) ".task")))

(defn create-task-file [conf task task-id]
  (spit (task-file conf task) task-id))

(defn read-when [file]
  (when
      (fs/exists? task-file) (slurp task-file)))

(defn active-task-id
  "Looks for a local task file representing the specified task.
   If it's found, returns the task id from that file.
   Otherwise, returns nil."
  [conf task]
  (let [task-file (task-file conf task)]
    (when
        (fs/exists? task-file) (slurp task-file))))

(defn task-queue [host port resource]
  (TaskQueue. host (Integer/parseInt port) resource))

(defn push-new-task
  "Pushes a new task using the specfied conf and task data.
   Saves the task's unique task id to a local file representing the task.
   Returns the task's unique task id."
  [{:keys [host port resource] :as conf} task]
  (let [q (task-queue host port resource)
        _ (info "drake.vineayrd/push-new-task: Pushing task to" host port resource "...")
        task-id (.addTask q task)]
    (create-task-file conf task task-id)
    (info "drake.vineayrd/push-new-task: Pushed task" task-id)
    task-id))

(defn done? [task]
  (contains? END-STATUS (.getStatusRemote task)))

(defn wait-for [{:keys [host port resource]} task-id]
  (let [task (.getTask (task-queue host port resource) task-id)]
    (loop []
      (when-not (done? task)
        (info "drake.vineayrd/run-task: waiting on task: " task-id "...")
        (Thread/sleep 5000)
        (recur)))
   (when (= vineyard.Task$Status/ERROR (.getStatusRemote task))
     (throw (Exception. (str "Error running Vineyard task: " task-id))))))

(defn run-task
  "Runs the specified task and waits for it to be DONE.
   Uses the local filesystem to track active task runs.
   If it looks like an identical task is currently running,
   will treat that as the task and wait for it to be DONE."
  [conf task]
  (let [task-id (or
                 (active-task-id conf task)
                 (push-new-task conf task))]
    (info "drake.vineayrd/run-task: waiting on task: " task-id "...")
    (try
      (wait-for conf task-id)
      (finally (fs/delete (task-file conf task))))))



(comment
  (defn test-push []
    (let [q (TaskQueue. "localhost" 8069 "test_resource")
          task-map {"type" "vineyard.EchoTask"
                    "impl" {"group-id" "factual"
                            "artifact" "vineyard-java-driver"
                            "version" "1.7.5"}}]
      (.addTask q task-map)))

  (defn get-status [task-id]
    (.getStatusRemote
     (.getTask (task-queue "localhost" "8069" "test_resource") task-id))))
