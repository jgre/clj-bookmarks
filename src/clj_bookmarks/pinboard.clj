(ns clj-bookmarks.pinboard
  "The `pinboard` namespace provides the implementation of the
  [Pinboard API](http://pinboard.in/howto/#api)."
  (:use [clj-bookmarks.core]
	[midje.sweet])
  (:require [clj-http.client :as http]
	    [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.contrib.zip-filter.xml :as zf]
	    [clojure.contrib.zip-filter :as zfilter]
	    [clojure.string :as string])
  (:import [java.net URL]
	   [java.util TimeZone Date]
	   [java.text SimpleDateFormat]
	   [java.io ByteArrayInputStream]
	   [java.security MessageDigest]))

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

(defn md5
  [str]
  {:post [(= 32 (count %))]}
  (let [md (MessageDigest/getInstance "MD5")
	msg (.getBytes str "UTF-8")
	out (.digest md msg)]
    (.. (BigInteger. 1 out) (toString 16))))

(defn request
  [srv url params]
  (http/get url {:query-params params
		 :basic-auth [(:user srv) (:passwd srv)]}))

(defn parse-tags
  [input]
  (vec (string/split input #"\s")))

(defn str->xmlzip
  [input]
  (-> input
      string->stream
      xml/parse
      zip/xml-zip))

(defn parse-posts
  [input]
  (zf/xml-> (str->xmlzip input) :post
	    (fn [loc] {:url (zf/attr loc :href)
		       :tags (parse-tags (zf/attr loc :tag))
		       :hash (zf/attr loc :hash)
		       :meta (zf/attr loc :meta)
		       :desc (zf/attr loc :description)
		       :extended (zf/attr loc :extended)
		       :date (parse-date (zf/attr loc :time))})))

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

(defn parse-result
  [input]
  (let [code (zf/xml1-> (str->xmlzip input) (zf/attr :code))]
    (if-not (= code "done")
      ;; FIXME a better error concept, maybe?
      (throw (Exception. code))
      true)))

(defn posts-add
  [srv url desc opts]
  (letfn [(opt->param
	   [[k v]]
	   (cond
	    (= k :tags) [:tags (if (coll? v) (string/join " " v) v)]
	    (isa? (class v) Date) [k (format-date v)]
	    (= k :shared)  [:shared  (if v "yes" "no")]
	    (= k :replace) [:replace (if v "yes" "no")]
	    :else [k v]))]
    (let [params (into {:url url :description desc} (map opt->param opts))]
      (-> (request srv (str *base-api-url* "posts/add") params)
	  :body
	  parse-result))))

(defn posts-get
  [srv url]
  (-> (request srv (str *base-api-url* "posts/get") {:url url})
      :body
      parse-posts))

(defn posts-delete
  [srv url]
  (-> (request srv (str *base-api-url* "posts/delete") {:url url})
      :body
      parse-result))

(defn parse-suggestions
  [input]
  (zf/xml-> (str->xmlzip input) zfilter/children zf/text))

(defn posts-suggest
  [srv url]
  (-> (request srv (str *base-api-url* "posts/suggest") {:url url})
      :body
      parse-suggestions))

(defn parse-update
  [input]
  (parse-date (zf/xml1-> (str->xmlzip input) (zf/attr :time))))

(defn posts-update
  [srv]
  (-> (request srv (str *base-api-url* "posts/update") {})
      :body
      parse-update))

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
  (add-bookmark [srv url desc opts] (posts-add srv url desc opts))
  (bookmark-info [srv url] (posts-get srv url))
  (delete-bookmark [srv url] (posts-delete srv url))
  (suggested-tags [srv url] (posts-suggest srv url))
  (last-update [srv] (posts-update srv)))


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

