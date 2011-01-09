(ns clj-bookmarks.test.util
  (:use [clj-bookmarks.util] :reload
	[midje.sweet])
  (:import [java.util Date]))

(fact
 ;; We initialize the date with a long value, because that is the only way to
 ;; construct a java.util.Date with a fixed value that is not deprecated and
 ;; does not require java.util.Calendar.
 ;; The value is equal to Jan 02 13:09:24 UTC 2011
 (let [d (Date. 1293973764173)
       dstr "2011-01-02T13:09:24Z"]
   (format-date d) => dstr
   (format-date (parse-date dstr)) => dstr))

