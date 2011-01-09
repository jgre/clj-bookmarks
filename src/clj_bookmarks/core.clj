(ns clj-bookmarks.core
  "The `core` namespace provides the common API for the different
  bookmarking services."
  )

;; ## The AnonymousBookmarkService protocol

(defprotocol AnonymousBookmarkService
  "The common functions for accessing a bookmark service anonymously
  provided through the `AnonymousBookmarkService` protocol."
  (bookmarks [srv opts]
	     "The `bookmarks` function retrieves bookmarks from the
	     service according to criteria specified in `opts`:

             * `user`: only return bookmarks saved by this user
             * `tags`: only return bookmarks tagged with an element of this
                       seq

             At least one option must be specified.")
  (popular [srv]
	   "To get the most popular bookmarks from the service, call
           `popular`.")
  (recent [srv]
	  "To get the most recent bookmarks saved to the service by
          all users, call `recent`."))

;; ## The AuthenticatedBookmarkService protocol

(defprotocol AuthenticatedBookmarkService
  "The common functions that implement calls to the different
  bookmarking APIs are bundled in the `BookmarkService` protocol."
  (query-bookmarks [srv opts]
	     "The `query-bookmarks` function retrieves bookmarks from the
	     service according to criteria specified in `opts`:

             * `user`: only return bookmarks saved by this user
             * `tags`: only return bookmarks tagged with an element of this
                       seq
             * `fromdt`: only return bookmarks saved on this date or later
             * `todt`: only return bookmarks saved on this date or earlier
             * `offset`: start returning bookmarks this many results into
                         the set
             * `limit`: return this many results

             When called with an empty `opts` map, `bookmarks` returns
             the 15 most recent bookmarks saved by the user.")
  (add-bookmark [srv url desc opts]
		"Bookmarks can be added by calling `add-bookmark` for
                the service, passing a URL, a description and a map of
                options that can have the following keys (none of them
                required):

                * `tags`: a seq of tags
                * `date`: the datestamp assigned to the bookmark
                          (default: now)
                * `shared`: make the item public (default: true)
                * `replace`: replace a bookmark if the given URL has
                             already been saved (default: true)")
  (delete-bookmark [srv url]
		   "The `delete-bookmark` function deletes the bookmark
                   for the given URL.")
  (suggested-tags [srv url]
		  "You can get a vector of suggestions for tags for a
                  given URL from the service using the
                  `suggested-tags` function.")
  (bookmark-info [srv url]
		 "Call `bookmark-info` to retrieve the bookmark
                 structure for `url`.")
  (last-update [srv]
		"Use `last-update` to find the timestamp when the user
		last updated his bookmarks."))

;; ## The Bookmark Structure
;;
;; The functions that return bookmarks always give you a map with the
;; following structure:
;;
;; * `url`: the bookmarked URL
;; * `tags`: the tags assigned to this bookmark
;; * `desc`: the description
;; * `extended`: Notes about the bookmark
;; * `date`: the time when the bookmark was saved
;; * `others`: the number of users who bookmarked the URL
;; * `hash`: the hash of the URL
;; * `meta`: a signature that changes when the bookmark is updated
;;
;; Only `url` is always set. Functions from the anonymous APIs never set
;; `extended`, `others`, `hash`, and `meta`.

