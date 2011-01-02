(defproject clj-bookmarks "1.0.0-SNAPSHOT"
  :description "A client library for for bookmarking services such as
[del.icio.us](http://delicious.com) or [Pinboard](http://pinboard.in).
It provides both annonymous and named access. In the former case the APIs
no not allow as many features as in the latter one."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [webmine "0.1.1"]
		 [midje "0.9.0RC1"]]
  :dev-dependencies [[marginalia "0.2.2"]])