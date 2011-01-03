(ns clj-bookmarks.core
  "The `core` namespace provides the common API for the different
  bookmarking services."
  )

;; ## Factory functions
;;
;; For each service there is a factory function (e.g. `init-pinboard`)
;; that returns a service handle. The handle is used for all
;; subsequent calls to the other functions.

(defn init-delicious
  "Create a service handle for [Delicious](http://delicious.com).

  When called without arguments, the [RSS
  API](http://www.delicious.com/help/feeds) is used which means that only a
  limited number of bookmarks will be shown. It also means that all
  writing functions will fail.

  When you pass a username and password, the full
  [API](http://www.delicious.com/help/api) is used so that all
  functions are available."
  ([] nil)
  ([user passwd] nil))

;; ## The BookmarkService protocol

(defprotocol BookmarkService
  "The common functions that implement calls to the different
  bookmarking APIs are bundled in the `BookmarkService` protocol."
  (bookmarks [srv opts]
	     "The `bookmarks` function retrieves bookmarks from the
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
  (popular [srv]
	   "To get the most popular bookmarks from the service call
           `popular`.")
  (recent [srv]
	  "To get the most recent bookmarks saved to the service by
          all users call `recent`.")
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
  (bookmark-info [srv url]
		 "Call `bookmark-info` to retrieve the bookmark
                 structure for `url`.")
  (delete-bookmark [srv url]
		   "The `delete-bookmark` function deletes the bookmark
                   for the given URL.")
  (suggested-tags [srv url]
		  "You can get a vector of suggestions for tags for a
                  given URL from the service using the
                  `suggested-tags` function.")
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