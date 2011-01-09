(ns clj-bookmarks.delicious
  "The `delicious` namespace provides the implementation of the
  [Delicious API](http://delicious.com)."
  (:use [clj-bookmarks core util])
  (:require [clj-http.client :as http]
	    [clojure.contrib.zip-filter.xml :as zfx]
	    [clojure.contrib.zip-filter :as zf]
	    [clojure.string :as string])
  (:import [java.util Date Locale]
	   [java.text SimpleDateFormat]))

;; ## The Delicious v1 API
;;
;; The following functions implement [version 1 of the Delicious
;; API](http://www.delicious.com/help/api) (which is also used by
;; Pinboard).
;;
;; ### Parser Functions
;;
;; The following functions parse the data returned from the service.

(defn parse-posts
  "Parse a string of XML data containg a list of posts into the
  bookmark structure.

  We first parse the data into a zipper, and extract the attributes of
  the `post` elements."
  [input]
  (zfx/xml-> (str->xmlzip input) :post
	    (fn [loc] {:url (zfx/attr loc :href)
		       :tags (parse-tags (zfx/attr loc :tag))
		       :hash (zfx/attr loc :hash)
		       :meta (zfx/attr loc :meta)
		       :desc (zfx/attr loc :description)
		       :extended (zfx/attr loc :extended)
		       :date (parse-date (zfx/attr loc :time))})))

(defn parse-result
  "Parse a string of XML data with a response code from the Delicious
  v1 API and either return true or throw an exception.

  The function returns true, when the `code` attribute equals
  `done`. Otherwise a exception is thrown with the code as message."
  [input]
  (let [code (zfx/xml1-> (str->xmlzip input) (zfx/attr :code))]
    (if-not (= code "done")
      ; FIXME a better error concept, maybe?
      (throw (Exception. code))
      true)))

(defn parse-suggestions
  "Parse a string of XML data into a seq of tags.

  We turn the data into a zipper and get the text of all leaf nodes."
  [input]
  (zfx/xml-> (str->xmlzip input) zf/children zfx/text))

(defn parse-update
  "Parse a string of XML data into the date of the last modification.

  We turn the input into a zipper and parse the date from the `time`
  attribute."
  [input]
  (parse-date (zfx/xml1-> (str->xmlzip input) (zfx/attr :time))))

;; ### Request Functions
;;
;; The functions in the section send requests to the service with
;; parameters formatted appropriately. The results are parsed using
;; the functions from the previous section.

(defn posts-all
  "Query bookmarks from the Delicious v1 API.

  This function sends a GET request to `posts/all` and appends some
  parameters as query-string. As we want to provide a convenient way
  to pass the parameters to this function, we need to convert the
  input `opts` before passing them on:

  * `user`: can be passed on as-is
  * `tags`: needs to be called `tag` for the request. Also a seq of tags
            must be converted to a space-delimited list.
  * `fromdt`: a `Date` object needs to be formatted
  * `todt`: same here
  * `offset`: called `start` in the API
  * `limit`: called `results` in the API

  The body of the response gets parsed into a seq of bookmark
  structures."
  [{:keys [endpoint] :as srv} opts]
  (letfn [(opt->param
	   [[k v]]
	   (cond
	    (= k :tags) [:tag (if (coll? v) (string/join " " v) v)]
	    (= k :limit)          [:results v]
	    (= k :offset)         [:start v]
	    (isa? (class v) Date) [k (format-date v)]
	    :else [k v]))]
    (let [params (into {} (map opt->param opts))]
      (-> (basic-auth-request srv (str endpoint "posts/all") params)
	  :body
	  (parse-posts)))))

(defn posts-add
  "Send a GET request to add a new bookmark with the Delicious v1 API.

  First, we need to convert `opts` again; `tags` into a
  space-delimited string, `date` from a `Date` object to a string, and
  boolean values `shared` and `replace` to `\"yes\"` or `\"no\"`.  The
  options are all optional, but the bookmark must get a URL and a
  description.

  Finally, we send the GET request, parse the result and raise an
  exception when something went wrong."
  [{:keys [endpoint] :as srv}
  url desc opts]
  (letfn [(opt->param
	   [[k v]]
	   (cond
	    (= k :tags) [:tags (if (coll? v) (string/join " " v) v)]
	    (isa? (class v) Date) [k (format-date v)]
	    (= k :shared)  [:shared  (if v "yes" "no")]
	    (= k :replace) [:replace (if v "yes" "no")]
	    :else [k v]))]
    (let [params
	  (into {:url url :description desc} (map opt->param opts))]
      (-> (basic-auth-request srv (str endpoint "posts/add") params)
	  :body
	  parse-result))))

(defn posts-get
  "Get the bookmark structure for `url`.

  Send a GET request to `posts/get` with the URL as query parameter,
  and than parse the response body to yield a list with zero or one
  bookmarks. Finally, call first to return either `nil` or the
  bookmark."
  [{:keys [endpoint] :as srv} url]
  (-> (basic-auth-request srv (str endpoint "posts/get") {:url url})
      :body
      parse-posts
      first))

(defn posts-delete
  "Delete a bookmark using the Delicious v1 API.

  Send a GET request to `posts/delete` with the URL as query
  parameter. The response body is parsed and we return `true` when it
  worked and throw an exception otherwise."
  [{:keys [endpoint] :as srv} url]
  (-> (basic-auth-request srv
			  (str endpoint "posts/delete") {:url url})
      :body
      parse-result))

(defn posts-suggest
  "Retrieve a list of suggested tags for a given URL using the
  Delicious v1 API.

  We send a GET request with the URL as query parameter to
  `posts/suggest`and parse the result body to get the seq of tags."
  [{:keys [endpoint] :as srv} url]
  (-> (basic-auth-request srv
			  (str endpoint "posts/suggest") {:url url})
      :body
      parse-suggestions))

(defn posts-update
  "Get the time of the last modification of an account using the
   Delicious v1 API.

  We send a GET request to `posts/update` and parse the result body
  into a `Date` object."
  [{:keys [endpoint] :as srv}]
  (-> (basic-auth-request srv (str endpoint "posts/update") {})
      :body
      parse-update))

;; ## The DeliciousV1Service Record
;; 
;; `DeliciousV1Service` implements the `AuthenticatedBookmarkService`
;; protocol for the Delicious v1 API. Requires authentication
;; (username, password).
;;
;; Internally the implementation is delegated to the functions above.

(defrecord DeliciousV1Service [endpoint user passwd]
  AuthenticatedBookmarkService
  (query-bookmarks [srv opts] (posts-all srv opts))
  (add-bookmark [srv url desc opts] (posts-add srv url desc opts))
  (bookmark-info [srv url] (posts-get srv url))
  (delete-bookmark [srv url] (posts-delete srv url))
  (suggested-tags [srv url] (posts-suggest srv url))
  (last-update [srv] (posts-update srv)))

;; ## The Delicious RSS Feeds
;;
;; The functions here are responsible for getting data out of the
;; Delicious RSS feeds.

(def *del-base-rss-url* "http://feeds.delicious.com/v2/rss/")

;; ### Parser Functions

(defn rss-date-format
  "Create a `SimpleDateFormat` object for the format used by the
  Delicious RSS feeds.

  As the format uses names for weekday and months, we need to set the
  locale to US."
  []
  (SimpleDateFormat. "EEE',' dd MMM yyyy HH:mm:ss Z" (Locale/US)))

(defn parse-rss-date
  "Parse a date string in the format used by the Delicious RSS feeds
  into a `Date` object."
  [input]
  (.parse (rss-date-format) input))

(defn parse-rss-posts
  "Parse a string of RSS data from Delicious into a list of posts.

  The input is turned into a zipper which we use to extract the data
  from the `item` elements. The fields we need are in sub-elements:

  * `link`: put verbatimly into `url` in the result
  * `category`: these are the tags which we gather into a vector
  * `title`: we call this `desc`
  * `pubDate`: this is parsed into a `Date` object and called `date`."
  [input]
  (zfx/xml-> (str->xmlzip input) :channel :item
	     (fn [loc] {:url (zfx/xml1-> loc :link zfx/text)
			:tags (vec
			       (zfx/xml-> loc :category zfx/text))
			:desc (zfx/xml1-> loc :title zfx/text)
			:date (parse-rss-date
				  (zfx/xml1-> loc :pubDate zfx/text))})))

;; ### Request Functions

(defn rss-popular
  "Get the currently popular bookmars using the Pinboard RSS feeds.

  We send a GET request to `popular` and parse the response body into
  a seq of bookmarks."
  []
  (-> (http/get (str *del-base-rss-url* "popular/"))
      :body
      parse-rss-posts))

(defn rss-recent
  "Get the recent bookmars using the Delicious RSS feeds.

  We send a GET request to `recent` and parse the response body into
  a seq of bookmarks."
  []
  (-> (http/get (str *del-base-rss-url* "recent/"))
      :body
      parse-rss-posts))

(defn rss-bookmarks
  "The `rss-bookmarks` function uses the RSS feeds to perform a query
  for shared bookmarks.

  The parameter map can include `tags` and `user`. `tags` can be
  either a string or a vector of string. The map must not be empty.

  This function sends a GET request with the path `/USER/TAG+TAG` or
  `/tag/TAG+TAG` when no user is specified."
  [{:keys [tags user]}]
  (let [tags (if (string? tags) tags (string/join "+" tags))
	path (string/join "/" (filter (comp not string/blank?)
				      [(or user "tag") tags]))]
    (-> (http/get (str *del-base-rss-url* path))
	:body
	parse-rss-posts)))

;; ## The DeliciousRSSService Record
;;
;; `DeliciousRSSService` implements the `BookmarkService` protocol for the
;; Delicious RSS feeds. No authentication is required.

(defrecord DeliciousRSSService []
  AnonymousBookmarkService
  (bookmarks [srv opts] (rss-bookmarks opts))
  (popular [srv] (rss-popular))
  (recent [srv] (rss-recent)))

(def *del-base-api-url* "https://api.del.icio.us/v1/")

(defn init-delicious
  "Create a service handle for [Delicious](http://delicious.com).

  When called without arguments, the [RSS
  API](http://www.delicious.com/help/feeds) is used.

  When you pass a username and password, the full
  [API](http://www.delicious.com/help/api) is used."
  ([] nil)
  ([user passwd] (init-delicious *del-base-api-url* user passwd))
  ([endpoint user passwd] (DeliciousV1Service. endpoint user passwd)))

