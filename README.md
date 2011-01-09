# clj-bookmarks

A client library for for the bookmarking services
[del.icio.us](http://delicious.com) and
[Pinboard](http://pinboard.in). It provides both annonymous and named
access. In the former case the APIs do not allow as many features as
in the latter one.

## Usage

The library offers a common APIs for the bookmarking services -- one
set of functions for authenticated access and another set that can be
used anonymously.

Before accessing any of the data, you need to initialize the service
you want to use and get a service handle.

### Anonymous Access ###

In order to create a service handle for the anonymous APIs, call a
factory function without arguments. For Pinboard use the following:

    (use '[clj-bookmarks core pinboard])
    (def pb (init-pinboard))

For Delicious:

    (use '[clj-bookmarks core delicious])
    (def del (init-delicious))

This creates a handle for the limited authenticated API.
(The following examples all use Pinboard, but they work the same with
Delicious.)  To get a list of the most popular bookmarks, call

    (popular pb)

This returns a seq of hashes where each element has the following
keys:

* `url`: the bookmarked URL
* `tags`: the tags assigned to this bookmark
* `desc`: the description
* `date`: the time when the bookmark was saved

The `recent` function gives you the list of all recetly saved
bookmarks. 

You can also query bookmarks by user and by tags.  When you want the
bookmarks by user the user "jgre" tagged "clojure" use the `bookmarks`
function:

    (bookmarks pb {:user "jgre" :tags "clojure"})

The result has the structure described above. You can also request the
bookmarks tagged "clojure" *and* "java":

    (bookmarks pb {:user "jgre" :tags ["java" "clojure"]})
    
### Authenticated Access ###

To use the authenticated API, you pass your username and password to
`init-pinboard` or `init-delicious`.

    (def del (init-delicious "USER" "PASSWD"))

The authenticated API has a `query-bookmarks` function that behaves
simliar to `bookmarks`, but they have some differences. Moreover,
Pinboard implements the service differently than Delicious. When you
specify a list of tags, Pinboard gives you the bookmarks that have at least
one of the tags. Delicious gives you the bookmarks that have all of
the tags. In the previous example you would get the bookmarks
tagged "clojure" *or* "java" from Pinboard, but those tagged "clojure"
*and* "java" from Delicious.

`query-bookmarks` also provides some additional options compared to
`bookmarks`:

* `fromdt`: only return bookmarks saved on this date or later
* `todt`: only return bookmarks saved on this date or earlier
* `offset`: start returning bookmarks this many results into the set
* `limit`: return this many results

The result structure has more fields too with this API:

* `url`: the bookmarked URL
* `tags`: the tags assigned to this bookmark
* `desc`: the description
* `extended`: Notes about the bookmark
* `date`: the time when the bookmark was saved
* `others`: the number of users who bookmarked the URL
* `hash`: the hash of the URL
* `meta`: a signature that changes when the bookmark is updated

With the authenticated API you have more functions at you
disposal. With `add-bookmark` you can save new bookmarks, you can
delete bookmarks with `delete-bookmarks`, and you can get suggestions
for how to tag a given URL with `suggested-tags`. `bookmark-info`
gives you the bookmark data structure for a given URL. When your
application caches a user's bookmarks, you can use `last-update` to
find out when the last update on the account happened to determine
when to reload.

## Installation

With leiningen, add this to you `project.clj`:

    [clj-bookmarks "0.1.0"]

For Maven, add this to your `pom.xml`:

    <dependency>
        <groupId>clj-bookmarks</groupId>
        <artifactId>clj-bookmarks</artifactId>
       <version>0.1.0</version>
    </dependency>

## License

Distributed under the [Simplified BSD License](http://www.opensource.org/licenses/bsd-license.php):

Copyright (C) 2010, Janico Greifenberg
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
