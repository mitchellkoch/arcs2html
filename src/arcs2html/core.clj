(ns arcs2html.core
  (:use [clojure.java.io :only [reader file copy]])
  (:import [org.jwat.arc ArcReaderFactory]
           [java.io FileInputStream]
           [java.net URLEncoder]))

(defn get-records [rdr]
  (lazy-seq
   (try
     (when-let [rec (.getNextRecord rdr)]
       (cons rec (get-records rdr)))
     (catch Throwable t []))))

(defn arc-file-seq [file]
  (do
    (println file)
    (try
      (let [rdr (ArcReaderFactory/getReader (FileInputStream. file))]
        (.getVersionBlock rdr)
        (get-records rdr))
      (catch Exception e
        (do (println *err* (str "Exception reading " file ": " e))
            [])))))

(defn arc-dir-seq [dir]
  (->> dir file file-seq
       rest ; don't use the directory itself
       (mapcat arc-file-seq)))

(defn content-seq [arc-dir]
  (->> (arc-dir-seq arc-dir)
       (map #(hash-map
              :url (.getRawUrl %)
              :date (.getRawArchiveDate %)
              :content-type (.getContentType %)
              :content-instream (.getInputStream (.getPayload %))))))

(defn -main [crawl-dir out-dir]
  (doseq [[id {:keys [url date content-type content-instream] :as content-rec}]
          (map vector (range) (content-seq crawl-dir))]
    (let [id-str (format "%07d" id)
          id-prefix (subs id-str 0 3)
          content-dir (str out-dir "/" id-prefix)
          enc-url (URLEncoder/encode url)
          trunc-enc-url (subs enc-url 0 (min 100 (count enc-url)))
          ext (case content-type
                "text/html" ".html"
                "application/pdf" ".pdf"
                "")
          filename (str content-dir "/"
                        id-str "-"
                        trunc-enc-url
                        (if (= ext (subs trunc-enc-url
                                         (- (count trunc-enc-url)
                                            (count ext))
                                         (count trunc-enc-url)))
                          ""
                          ext))]
      (.mkdirs (file content-dir))
      (copy content-instream (file filename))
      (.close content-instream))))
