(ns clj-bookmarks.test.pinboard
  (:use [clj-bookmarks.pinboard] :reload
	[midje.sweet])
  (:import [java.util Date]))

(fact
 ;; We initialize the date with a long value, because that is the only way to
 ;; construct a java.util.Date with a fixed value that is not deprecated and
 ;; does not require java.util.Calendar.
 ;; The value is equal to Jan 02 13:09:24 UTC 2011
 (let [d (Date. 1293973764173)]
   (format-date d) => "2011-01-02T13:09:24Z"))

;; Test data
(def javadoc-posts
     "
<?xml version='1.0' encoding='UTF-8' ?>
<posts user='jgre'>
  <post href='http://scala-tools.org/mvnsites-snapshots/liftweb/lift-base/lift-webkit/scaladocs/index.html' time='2009-12-17T19:36:45Z' description='Lift WebKit 1.1-SNAPSHOT API : net.liftweb.http.LiftRules' extended='' tag='javadoc scala lift' hash='55505d5e761702c83b8737fbdef13f5a' meta='84aaa77e9a746412bae39b8bfb1a684e'   />
  <post href='http://java.sun.com/javase/6/docs/api/' time='2009-03-07T19:29:25Z' description='java.io (Java Platform SE 6)' extended='' tag='programming api java javadoc' hash='dfde7fa8611fb1176a1a13bb812f90d6' meta='03bee7cc22b1fded570c0d5f98288e77'   />
</posts>")

(fact
 (posts-all ...srv... {:tags "javadoc"}) =>
 (just [(contains {:url "http://java.sun.com/javase/6/docs/api/"
		   :tags ["programming" "api" "java" "javadoc"]
		   :hash "dfde7fa8611fb1176a1a13bb812f90d6"
		   :meta "03bee7cc22b1fded570c0d5f98288e77"
		   :desc "java.io (Java Platform SE 6)"})
	;; XXX: testing the equality of dates is just too tedious
	(contains {:url "http://scala-tools.org/mvnsites-snapshots/liftweb/lift-base/lift-webkit/scaladocs/index.html"})] :in-any-order)
 (provided
  (request ...srv... anything {:tag "javadoc"}) => {:body javadoc-posts}))

(fact
 (posts-all ...srv... {:tags ["javadoc" "programming"]
		       ;; Jan 01 00:00:00 UTC 2009
		       :fromdt (Date. 1230768000000)
		       ;; Dec 31 00:00:00 UTC 2009
		       :todt (Date. 1262217600000)
		       :user "jgre"
		       :offset 0
		       :limit 5})
 =>
 (contains [(contains {:url "http://java.sun.com/javase/6/docs/api/"})])
 (provided
  (request ...srv... anything {:tag     "javadoc programming"
			       :fromdt  "2009-01-01T00:00:00Z"
			       :todt    "2009-12-31T00:00:00Z"
			       :user    "jgre"
			       :start   0
			       :results 5})
  => {:body javadoc-posts}))

(def done-xml "
<?xml version='1.0' encoding='UTF-8' ?>
    <result code='done' />
<!-- generated 01/03/11 18:01:45 UTC -->
")

(def error-xml "
<?xml version='1.0' encoding='UTF-8' ?>
    <result code='something went wrong' />
<!-- generated 01/03/11 18:01:09 UTC -->
")

(fact
 (parse-result done-xml) => truthy
 (parse-result error-xml) => (throws Exception "something went wrong"))

(def suggest-xml "
<?xml version='1.0' encoding='UTF-8' ?>
<suggested>
	<popular>data</popular>
	<popular>database</popular>
	<popular>mapreduce</popular>
	<recommended>hadoop</recommended>
	<recommended>bigdata</recommended>
	<recommended>data</recommended>
	<recommended>mapreduce</recommended>
	<recommended>database</recommended>
	<recommended>storage</recommended>
	<recommended>nosql</recommended>
	<recommended>smaq</recommended>
	<recommended>datamining</recommended>
	<recommended>query</recommended>
</suggested>
")

(fact
 (parse-suggestions suggest-xml) =>
 (contains ["data" "mapreduce" "hadoop" "smaq" "query"] :gaps-ok))

(def update-xml "
<?xml version='1.0' encoding='UTF-8'?><update time='2011-01-03T19:15:09Z' />
")

(fact
 (parse-update update-xml) => #(= (.getTime %) 1294082109000))
