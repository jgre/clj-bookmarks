(ns clj-bookmarks.test.delicious
  (:use [clj-bookmarks delicious util] :reload
	[midje.sweet])
  (:import [java.util Date]))

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
  (basic-auth-request ...srv... anything {:tag "javadoc"}) =>
  {:body javadoc-posts}))

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
  (basic-auth-request ...srv... anything {:tag     "javadoc programming"
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

(def recent-xml "
<?xml version='1.0' encoding='UTF-8'?>
<rss version='2.0' xmlns:atom='http://www.w3.org/2005/Atom' xmlns:content='http://purl.org/rss/1.0/modules/content/' xmlns:wfw='http://wellformedweb.org/CommentAPI/' xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns:dc='http://purl.org/dc/elements/1.1/' xmlns:cc='http://web.resource.org/cc/'>
  <channel>
    <title>Delicious popular</title>
    <link>http://www.delicious.com/popular</link>
    <description>the latest popular bookmarks</description>
    <atom:link rel='self' type='application/rss+xml' href='http://feeds.delicious.com/v2/rss/popular/'/>
    <item>
      <title>Top 40: Exceptional Images from the Decay Photography Challenge</title>
      <pubDate>Sat, 08 Jan 2011 21:46:20 +0000</pubDate>
      <guid isPermaLink='false'>http://www.delicious.com/url/5b15c9c2cbf49f9f7bd84853198576b1#hrvojecar</guid>
      <link>http://top-lists.info/top-40-exceptional-images-from-the-decay-photography-challenge</link>
      <dc:creator><![CDATA[hrvojecar]]></dc:creator>
      <comments>http://www.delicious.com/url/5b15c9c2cbf49f9f7bd84853198576b1</comments>
      <wfw:commentRss>http://feeds.delicious.com/v2/rss/url/5b15c9c2cbf49f9f7bd84853198576b1</wfw:commentRss>
      <source url='http://feeds.delicious.com/v2/rss/hrvojecar'>hrvojecar's bookmarks</source>
      <category domain='http://www.delicious.com/hrvojecar/'>challenge</category>
      <category domain='http://www.delicious.com/hrvojecar/'>decay</category>
      <category domain='http://www.delicious.com/hrvojecar/'>photography</category>
      <category domain='http://www.delicious.com/hrvojecar/'>top</category>
      <category domain='http://www.delicious.com/hrvojecar/'>image</category>
      <category domain='http://www.delicious.com/hrvojecar/'>photos</category>
      <category domain='http://www.delicious.com/hrvojecar/'>images</category>
    </item>
    <item>
      <title>mir.aculo.us JavaScript with Thomas Fuchs » DOM Monster Bookmarklet</title>
      <pubDate>Sat, 08 Jan 2011 16:32:06 +0000</pubDate>
      <guid isPermaLink='false'>http://www.delicious.com/url/3577a509a90511c9103ed945d3002c9d#markgibaud</guid>
      <link>http://mir.aculo.us/dom-monster/</link>
      <dc:creator><![CDATA[markgibaud]]></dc:creator>
      <comments>http://www.delicious.com/url/3577a509a90511c9103ed945d3002c9d</comments>
      <wfw:commentRss>http://feeds.delicious.com/v2/rss/url/3577a509a90511c9103ed945d3002c9d</wfw:commentRss>
      <source url='http://feeds.delicious.com/v2/rss/markgibaud'>markgibaud's bookmarks</source>
      <category domain='http://www.delicious.com/markgibaud/'>javascript</category>
      <category domain='http://www.delicious.com/markgibaud/'>performance</category>
      <category domain='http://www.delicious.com/markgibaud/'>dom</category>
      <category domain='http://www.delicious.com/markgibaud/'>tools</category>
      <category domain='http://www.delicious.com/markgibaud/'>html</category>
      <category domain='http://www.delicious.com/markgibaud/'>webdev</category>
      <category domain='http://www.delicious.com/markgibaud/'>bookmarklet</category>
      <category domain='http://www.delicious.com/markgibaud/'>debugging</category>
      <category domain='http://www.delicious.com/markgibaud/'>webdesign</category>
      <category domain='http://www.delicious.com/markgibaud/'>js</category>
    </item>
  </channel>
</rss>
")

(fact
 (parse-rss-posts recent-xml) =>
 (just [(contains {:url "http://top-lists.info/top-40-exceptional-images-from-the-decay-photography-challenge"
		   :desc "Top 40: Exceptional Images from the Decay Photography Challenge"
		   :tags ["challenge" "decay" "photography" "top" "image" "photos" "images"]})
	(contains {:url "http://mir.aculo.us/dom-monster/"})]))

(fact
 ;; Dec 28 06:55:53 UTC 2010
 (.getTime (parse-rss-date "Sat, 08 Jan 2011 16:32:06 +0000")) =>
 1294504326000)
