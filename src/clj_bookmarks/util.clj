(ns clj-bookmarks.util
  "Common utility functions live in the 'util' namespace."
  (:require [clj-http.client :as http]
	    [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.contrib.zip-filter.xml :as zf]
	    [clojure.contrib.zip-filter :as zfilter]
	    [clojure.string :as string])
  (:import [java.util TimeZone Date]
	   [java.text SimpleDateFormat]
	   [java.io StringReader]
	   [org.xml.sax InputSource]))

(defn string->input-source
  "Covert a string into a SAX InputSource.

  In order to parse XML data from a string, you need to put it into an
  `InputSource` object. When you pass a string to `clojure.xml/parse`
  it interprets it as a filename."
  [s]
  (InputSource. (StringReader. (.trim s))))

(defn str->xmlzip
  "Turn a string of XML data into a zipper structure."
  [input]
  (-> input
      string->input-source
      xml/parse
      zip/xml-zip))

(defn date-format
  "Create a `SimpleDateFormat` object for the format used by the
  Delicious v1 API.

  The format object needs to be set to the UTC timezone, otherwise it
  would use the default timezone of the current machine."
  []
  (doto (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn format-date
  "Convert a `Date` object into a string with the format expected by
  the Delicious v1 API."
  [d]
  (.format (date-format) d))

(defn parse-date
  "Parse a date string in the format used by the Delicious v1 API into
  a `Date` object."
  [input]
  (.parse (date-format) input))

(defn basic-auth-request
  "Send an HTTP GET request using basic authentication to the given
  URL to which `params` get attached.

  The first argument is a map with the keys `user` and `passwd` used
  for the authentication (usually the service handle records are used
  here)."
  [{:keys [user passwd]} url params]
  (http/get url {:query-params params
		 :basic-auth [user passwd]}))

(defn parse-tags
  "Parse a space delimited string of tags into a vector."
  [input]
  (vec (string/split input #"\s")))

