(defproject clj-bookmarks "0.1.0"
  :description "A client library for for bookmarking services such as
[del.icio.us](http://delicious.com) or [Pinboard](http://pinboard.in).
It provides both annonymous and named access. In the former case the APIs
do not allow as many features as in the latter one."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clj-http "0.1.3"]
		 [midje "0.9.0"]]
  :dev-dependencies [[marginalia "0.3.0"]])

