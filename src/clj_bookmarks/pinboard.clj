(ns clj-bookmarks.pinboard
  "The `pinboard` namespace provides the implementation of the
  [Pinboard API](http://pinboard.in/howto/#api)."
  (:use [clj-bookmarks core util])
  (:require [clj-http.client :as http]
	    [clojure.contrib.zip-filter.xml :as zfx]
	    [clj-bookmarks.delicious :as del]
	    [clojure.string :as string])
  (:import [java.util TimeZone Date]
	   [java.text SimpleDateFormat]))

(def *pb-base-api-url* "https://api.pinboard.in/v1/")

;; ## The Pinboard RSS Feeds
;;
;; The functions here are responsible for getting data out of the
;; Pinboard RSS feeds.

(def *pb-base-rss-url* "http://feeds.pinboard.in/rss/")

;; ### Parser Functions

(defn rss-date-format
  "Create a `SimpleDateFormat` object for the format used by the
  Pinboard RSS feeds.

  The format object needs to be set to the UTC timezone, otherwise it
  would use the default timezone of the current machine. The dates in
  the RSS do provide a timezone, but it is always UTC and it is
  formatted in a way that `SimpleDateFormat` does not seem to support:
  In the feed there is an appended `+00:00`, but we could only parse
  either `GMT+00:00` or `+0000`."
  []
  (doto (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss")
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn parse-rss-date
  "Parse a date string in the format used by the Pinboard RSS feeds
  into a `Date` object."
  [input]
  (.parse (rss-date-format) input))


(defn parse-rss-posts
  "Parse a string of RSS data from Pinboard into a list of posts.

  The input is turned into a zipper which we use to extract the data
  from the `item` elements. The fields we need are in sub-elements:

  * `link`: put verbatimly into `url` in the result
  * `dc:subject`: these are the tags which we split into a vector
  * `description`: we call this `desc`
  * `dc:date`: this is parsed into a `Date` object and called `date`."
  [input]
  (zfx/xml-> (str->xmlzip input) :item
	    (fn [loc] {:url (zfx/xml1-> loc :link zfx/text)
		       :tags (parse-tags
			      (zfx/xml1-> loc :dc:subject zfx/text))
		       :desc (zfx/xml1-> loc :description zfx/text)
		       :date (parse-rss-date
			      (zfx/xml1-> loc :dc:date zfx/text))})))

;; ### Request Functions

(defn rss-popular
  "Get the currently popular bookmars using the Pinboard RSS feeds.

  We send a GET request to `popular` and parse the response body into
  a seq of bookmarks."
  []
  (-> (http/get (str *pb-base-rss-url* "popular/"))
      :body
      parse-rss-posts))

(defn rss-recent
  "Get the recent bookmars using the Pinboard RSS feeds.

  We send a GET request to `recent` and parse the response body into
  a seq of bookmarks."
  []
  (-> (http/get (str *pb-base-rss-url* "recent/"))
      :body
      parse-rss-posts))

(defn rss-bookmarks
  "The `rss-bookmarks` function uses the RSS feeds to perform a query
  for shared bookmarks.

  The parameter map can include `tags` and `user`. `tags` can be
  either a string or a vector of string. The map must not be empty.

  This function sends a GET request with the path `/u:USER/t:TAG/t:TAG`."
  [{:keys [tags user]}]
  (let [tags (if (string? tags) [tags] tags)
	path (string/join "/" (filter (comp not nil?)
				      (cons  (if user (str "u:" user))
					     (map #(str "t:" %) tags))))]
    (-> (http/get (str *pb-base-rss-url* path))
	:body
	parse-rss-posts)))

;; ## The PinboardRSSService Record
;;
;; `PinboardRSSService` implements the `BookmarkService` protocol for the
;; Pinboard RSS feeds. No authentication is required.

(defrecord PinboardRSSService []
  AnonymousBookmarkService
  (bookmarks [srv opts] (rss-bookmarks opts))
  (popular [srv] (rss-popular))
  (recent [srv] (rss-recent)))

(defn init-pinboard
  "Create a service handle for [Pinboard](http://pinboard.in).

  When called without arguments, the [RSS
  feeds](http://pinboard.in/howto/#rss) are used.

  When you pass a username and password, the
  [API](http://pinboard.in/howto/#api) (which is modeled on the
  Delicious API) is used."
  ([] (PinboardRSSService.))
  ([user passwd] (del/init-delicious *pb-base-api-url* user passwd)))

