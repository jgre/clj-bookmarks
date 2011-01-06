# clj-bookmarks

A client library for for the bookmarking services
[del.icio.us](http://delicious.com) and
[Pinboard](http://pinboard.in). It provides both annonymous and named
access. In the former case the APIs do not allow as many features as
in the latter one.

## Usage

The library offers a common API for the different bookmarking
services. Before accessing any of the data, you need to initialize the
service you want to use and get a service handle.

### Pinboard ###

For Pinboard use the following:

    (use '[clj-bookmarks core pinboard])
    (def pb (init-pinboard))

This creates a handle for the limited authenticated API. To get a list
of the most popular bookmarks, call

    (popular pb)

This returns a seq of hashes where each element has the following
keys:

* `url`: the bookmarked URL
* `tags`: the tags assigned to this bookmark
* `desc`: the description
* `date`: the time when the bookmark was saved

When you want the bookmarks by a given user tagged "clojure" use the
`bookmarks` function:

    (bookmarks pb {:user "jgre" :tags "clojure"})

The result has the form described above. You can also request the
bookmarks tagged "clojure" *and* "java":

    (bookmarks pb {:user "jgre" :tags ["java" "clojure"]})
    
To use the authenticated API, you pass your username and password to
`init-pinboard`. The `bookmarks` function behaves slightly different
in this implementation: when you specify a list of tags, it gives you
the bookmarks that have at least one of the tags. In the previous
example you would get the bookmarks tagged "clojure" *or* "java".

With the authenticated API you have more functions at you
disposal. With `add-bookmark` you can save new bookmarks, you can
delete bookmarks with `delete-bookmarks`, and you can get suggestions
for how to tag a given URL with `suggested-tags`. For a complete list
of available functions, see FIXME.

### Delicious ###

Access to delicious behaves analogously. Instead of `init-pinboard`,
use `init-delicious`.

## Installation

FIXME

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
