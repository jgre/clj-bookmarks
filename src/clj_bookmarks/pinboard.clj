(ns clj-bookmarks.pinboard
  "The `pinboard` namespace provides the implementation of the
  [Pinboard API](http://pinboard.in/howto/#api)."
  (:use [clj-bookmarks.core]
	[midje.sweet])
  (:require [clj-http.client :as http]
	    [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.contrib.zip-filter.xml :as zf]
	    [clojure.string :as string])
  (:import [java.net URL]
	   [java.util TimeZone Date]
	   [java.text SimpleDateFormat]
	   [java.io ByteArrayInputStream]))

(def *base-rss-url* "http://feeds.pinboard.in/rss/")
(def *base-api-url* "https://api.pinboard.in/v1/")

(defn string->stream
  [s]
  (ByteArrayInputStream. (.getBytes (.trim s))))

(defn date-format
  []
  (doto (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn format-date
  [d]
  (.format (date-format) d))

(defn parse-date
  [input]
  (.parse (date-format) input))

(defn request
  [srv url params]
  (http/get url {:query-params params
		 :basic-auth [(:user srv) (:passwd srv)]}))

(defn parse-tags
  [input]
  (vec (string/split input #"\s")))

(defn parse-posts
  [input]
  (-> input
      string->stream
      xml/parse
      zip/xml-zip
      (zf/xml-> :post
		(fn [loc] {:url (zf/attr loc :href)
			   :tags (parse-tags (zf/attr loc :tag))
			   :hash (zf/attr loc :hash)
			   :meta (zf/attr loc :meta)
			   :desc (zf/attr loc :description)
			   :extended (zf/attr loc :extended)
			   :date (parse-date (zf/attr loc :time))}))))

(defn posts-all
  [srv opts]
  (letfn [(opt->param
	   [[k v]]
	   (cond
	    (= k :tags)           [:tag (if (coll? v) (string/join " " v) v)]
	    (= k :limit)          [:results v]
	    (= k :offset)         [:start v]
	    (isa? (class v) Date) [k (format-date v)]
	    :else [k v]))]
    (let [params (into {} (map opt->param opts))]
      (-> (request srv (str *base-api-url* "posts/all") params)
	  :body
	  (parse-posts)))))

;; ## The PinboardService Record
;; 
;; `PinboardService` implements the `BookmarkService` protocol for Pinboard.

(defrecord PinboardService [user passwd]
  BookmarkService
  (bookmarks [srv opts]
	    ;; Retrieving bookmarks uses the `/posts/all` API call
	     (posts-all srv opts))
  (popular [srv] nil)
  (recent [srv] nil)
  (add-bookmark [srv url opts] nil)
  (bookmark-info [srv url] nil)
  (delete-bookmark [srv url] nil)
  (suggested-tags [srv url] nil)
  (last-update [srv]))


(defn init-pinboard
  "Create a service handle for [Pinboard](http://pinboard.in).

  When called without arguments, the [RSS
  API](http://pinboard.in/howto/#rss) is used which means that only a
  limited number of bookmarks will be shown. It also means that all
  writing functions will fail.

  When you pass a username and password, the full
  [API](http://pinboard.in/howto/#api) (which is modeled on the
  Delicious API) is used so that all functions are available."
  ([] nil)
  ([user passwd] (PinboardService. user passwd)))

