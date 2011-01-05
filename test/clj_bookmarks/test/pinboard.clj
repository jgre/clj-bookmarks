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

(def popular-xml "
<?xml version='1.0' encoding='UTF-8'?>
 <rdf:RDF xmlns='http://purl.org/rss/1.0/' xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns:content='http://purl.org/rss/1.0/modules/content/' xmlns:taxo='http://purl.org/rss/1.0/modules/taxonomy/' xmlns:dc='http://purl.org/dc/elements/1.1/' xmlns:cc='http://web.resource.org/cc/' xmlns:syn='http://purl.org/rss/1.0/modules/syndication/' xmlns:admin='http://webns.net/mvcb/'>
  <channel rdf:about='http://pinboard.in'>
    <title>Pinboard ()</title>
    <link>http://pinboard.in/u:</link>
    <description>popular items from Pinboard</description>
    <items>
      <rdf:Seq>	<rdf:li rdf:resource='http://duartes.org/gustavo/blog/post/what-your-computer-does-while-you-wait'/>
	<rdf:li rdf:resource='http://www.gutenberg.org/wiki/Main_Page'/>
	<rdf:li rdf:resource='http://meld.sourceforge.net/'/>
       </rdf:Seq>
    </items>
  </channel><item rdf:about='http://duartes.org/gustavo/blog/post/what-your-computer-does-while-you-wait'>
    <title>What Your Computer Does While You Wait : Gustavo Duarte</title>
    <dc:date>2010-12-28T06:55:53+00:00</dc:date>
    <link>http://duartes.org/gustavo/blog/post/what-your-computer-does-while-you-wait</link>
    <dc:creator></dc:creator>
    <description><![CDATA[  ]]></description>
    <dc:subject>reference to-read </dc:subject>
    <taxo:topics>
      <rdf:Bag>
      	<rdf:li rdf:resource='http://pinboard.in/u:/t:reference'/>
	<rdf:li rdf:resource='http://pinboard.in/u:/t:to-read'/>
        </rdf:Bag>
      </taxo:topics>
    </item><item rdf:about='http://lamb.cc/typograph/'>
    <title>Typograph (lamb.cc)</title>
    <dc:date>2010-12-28T06:55:53+00:00</dc:date>
    <link>http://lamb.cc/typograph/</link>
    <dc:creator></dc:creator>
    <description><![CDATA[ Typography scale and vertical tempo.
 ]]></description>
    <dc:subject>typography webdesigntools webdesign </dc:subject>
    <taxo:topics>
      <rdf:Bag>
      	<rdf:li rdf:resource='http://pinboard.in/u:/t:typography'/>
	<rdf:li rdf:resource='http://pinboard.in/u:/t:webdesigntools'/>
	<rdf:li rdf:resource='http://pinboard.in/u:/t:webdesign'/>
        </rdf:Bag>
      </taxo:topics>
    </item>
   </rdf:RDF>
")

(fact
  (parse-rss-posts popular-xml) =>
  (just [(contains {:url "http://duartes.org/gustavo/blog/post/what-your-computer-does-while-you-wait"
		    :tags ["reference" "to-read"]})
	 (contains {:url "http://lamb.cc/typograph/"
		    :desc " Typography scale and vertical tempo. "
		    :tags ["typography" "webdesigntools" "webdesign"]})]))

(fact
 ;; Dec 28 06:55:53 UTC 2010
 (.getTime (parse-rss-date "2010-12-28T06:55:53+00:00")) => 1293519353000)