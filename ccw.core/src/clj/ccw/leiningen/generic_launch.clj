(ns ccw.leiningen.generic-launch
  (:require [clojure.string :as str]
            [ccw.leiningen.launch :as launch]
            
            [ccw.util.swt :as swt]
            [ccw.util.eclipse :as e])
  (:import [org.eclipse.swt SWT]
           [org.eclipse.swt.widgets Text]))

(defn run-lein 
  "The command line can have a project name prepended, separated from the command
   by a $, such as in `project $ lein help`.
   It is optional to type `lein`: `project $ lein help` and `project $ help`
   are equivalent.
   Spaces around project and lein and around $ will be ignored

   no-project is the String used if no project must be used"
  [project no-project command]
  
  (let [[_ project-name args] (re-find #"(?:(.*)\$)?\s*(?:lein)?(.*)"
                                       command)
        project-name (and project-name (str/trim project-name))
        project-name (when (not= no-project project-name)
                       project-name)
        project-name (or project-name 
                         (and project (.getName project)))
        args (str/trim args)]
    (launch/lein project-name args)))

(defn generic-launch 
  "Open a popup asking the user the Leiningen command to issue,
   and then dynamically create a Java launch configuration invoking the command"
  [project]
  (swt/ui display
    (let [dialog	 (doto (swt/new-shell display
                                       SWT/ON_TOP
                                       SWT/TITLE
                                       SWT/CLOSE
                                       SWT/APPLICATION_MODAL)
                    (.setText "Leiningen command line")
                    (.setLayout (swt/form-layout :spacing 0
                                                 :margin-left 5
                                                 :margin-right 5
                                                 :margin-bottom 5
                                                 :margin-top 5))
              (.addListener SWT/Traverse
                (swt/listener e
                  (when (= SWT/TRAVERSE_ESCAPE (.detail e))
                    (.close (.widget e))
                    (set! (.detail e) SWT/TRAVERSE_NONE)
                    (set! (.doit e) false)))))
          no-project "<no project>"
          prompt (str (if project (.getName project) no-project) " $ lein ")
          command-input (doto (Text. dialog 0)
                          (.setText (str prompt "<task>"))
                          (.setToolTipText "Click Enter to execute, Click Esc to cancel")
                          (.setLayoutData (swt/form-data :width 400)))
          _ (doto command-input
              (.setSelection 
                (count prompt)
                (count (.getText command-input)))
              (.addKeyListener
                (swt/key-listener e
                  (when (= \return (.character e))
                    (run-lein project no-project (.getText command-input))
                    (.close dialog)))))
          cursor (.getCursorLocation display)]
      (doto dialog
        .pack
        (.setLocation (.x cursor) (.y cursor))
        .open)
      (.setFocus command-input))))