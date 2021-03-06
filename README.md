[![Gittip button](http://img.shields.io/gittip/arrdem.svg)](https://www.gittip.com/arrdem/ "Support this project")

# Lib-Grimoire

A small library for sharing code between [grimoire](https://github.com/clojure-grimoire/grimoire) and [lein-grim](https://github.com/clojure-grimoire/lein-grim) as well as other project interested in interacting with the Grimoire datastore or simply storing and retrieving Clojure source metadata.
lib-grimoire privides a shared abstraction of a Clojure Maven entity and a set of operations on various backends for reading and writing data using these entities.

## Installation

[![Clojars Project](http://clojars.org/org.clojure-grimoire/lib-grimoire/latest-version.svg)](http://clojars.org/org.clojure-grimoire/lib-grimoire)

Note that lib-grimoire is still pre-1.0.0, and as such is still in development and may undergo breaking changes.
Please refer to the [Changelog](#changelog) section.
Note that the SemVer model is followed strictly.
Upgrades between patches should be possible with no modifications to user code.
Upgrades between minor versions until 1.0.0 may involve breaking changes and should be expected to break.
After 1.0.0 upgrades between minor versions should be possible with no modifications to user code.
**IF I BREAK YOUR CODE** let me know, I'll buy you a beer in penance and will get shit fixed.

Now on to the code itself.

## Usage

### Things

Things are a structure used for uniquely naming versioned, vendored, platformed definitons and all parent structures thereof.
Defined in `grimoire.things` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.things) as follows:

```
Thing     ::= Sum[Group, Artifact, Version, Platform, Namespace, Def];
Group     ::= Record[                   Name: String];
Artifact  ::= Record[Parent: Group,     Name: String];
Version   ::= Record[Parent: Artifact,  Name: String];
Platform  ::= Record[Parent: Version,   Name: String];
Namespace ::= Record[Parent: Platform,  Name: String];
Def       ::= Record[Parent: Namespace, Name: String];
```

"Things" form a singly directed child to parent graph.
The various back end listing operations in `grimoire.api` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.api) provide downwards traversal of this graph by constructing children from parents.

#### Ex. 0

```Clojure
user> (require '[grimoire.things :as t])
nil
user> (-> (t/->Group "org.clojure")
           (t/->Artifact "clojure"))
#g/t [:grimoire.things/artifact
      {:parent
       #g/t [:grimoire.things/group
             {:name "org.clojure",
              :grimoire.things/url "org.clojure"}),
       :name "clojure",
       :grimoire.things/url "org.clojure/clojure"}]
user> (map t/thing->name (result (api/list-versions config *1)))
("1.7.0-alpha4" "1.7.0-alpha3" "1.7.0-alpha2" "1.7.0-alpha1" "1.6.0" "1.5.0" "1.4.0")
```

Nodes in this graph may be arbitrarily reconstructed from URI strings via `grimoire.things/path->thing` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.things/path->thing).

#### Ex. 1

```Clojure
user> (t/path->thing "org.clojure-grimoire/lib-grimoire/0.8.2")
#g/t [:grimoire.things/version
      {:parent
       #g/t [:grimoire.things/artifact
             {:parent
              #g/t [:grimoire.things/group
                    {:name "org.clojure-grimoire",
                    :grimoire.things/url "org.clojure-grimoire"}),
              :name "lib-grimoire",
              :grimoire.things/url "org.clojure-grimoire/lib-grimoire"}],
            :name "0.8.2",
            :grimoire.things/url "org.clojure-grimoire/lib-grimoire/0.8.2"}]
```

Note that doing so is _not_ generally type safe and is thus to be avoided unless you have good reason for doing so such as comparative efficiency of doing a lookup.

The `grimoire.things/thing->parent` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.things/thing->parent) provides upwards traversal by walking to and returning the parent node.
Using the same REPL from last time,

#### Ex. 2

```Clojure
user> (t/thing->parent *1)
#g/t [:grimoire.things/artifact
      {:parent
       #g/t [:grimoire.things/group
             {:name "org.clojure-grimoire",
              :grimoire.things/url "org.clojure-grimoire"}],
       :name "lib-grimoire",
       :grimoire.things/url "org.clojure-grimoire/lib-grimoire"}]
```

Every `Thing` can have attached metadata.
For some `Thing`s such as `Def`s, this metadata includes line numbers, added artifact version, deprecated artifact version and soforth.
Different back ends provide mechanisms for reading and writing metadata via `grimoire.api/read-meta` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.api/read-meta) and `grimoire.api/write-meta` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.api/write-meta).
Any `Thing` returned by the any part of the API _must_ have metadata, even if it's the empty map (or nil), associated with it and accessible via `read-meta`.
This contract is stated in the `grimoire.api` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.api), and in the `grimoire.api/read-meta` documentation.

#### Ex. 3

```Clojure
user> (keys
         (result
		   (api/read-meta
		     (lib-grim-config)
             (t/path->thing "org.clojure/clojure/1.6.0/clj/clojure.core/for"))))
(:added :ns :name :file :type :src :column :line :macro :arglists :doc)
```

The documentation for `write-meta` includes a list of keys which are expected to be set.

In addition to the above "locator" `Thing`s, two more exist as of 0.8.

```
Note    ::= Record[Parent: Thing, Handle: String];
Example ::= Record[Parent: Thing, Handle: String];
```

`Note` things encode datastore dependent handles (the meaning of which is not guaranteed at all).
`Note`s may be used to read a note, or write another `Note` text to the same "location".
The same goes for `Example`s.
These datums are only provisionally `Thing`s, as they could be attached to any arbitrary `Thing` as a parent.
See the documentation of specific back ends for information on where examples and notes are supported.
Examples are demanded only on `Def`s.
Notes are demanded only on `Σ[Artifact, Version, Platform, Ns, Def]`.
A back end _may_ choose to extend these nodes to arbitrary parents.

## Back ends

- [FS back end](#fs%-back-end)
- [Grimoire back end](#grimoire-back-end)

### FS back end

This is back end (`grimoire.api.fs.read` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.api.fs.read), `grimoire.api.fs.write` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.api.fs.write)) implements reading and writing on a filesystem datastore as used in Grimoire 0.4 and generated by lib-grimoire.
Load the reader and writer as desired and then use the API exposed in `grimoire.api` as needed.

This back end uses a configuration value as such:

#### Ex. 4

```Clojure
=> (require '[grimoire.api.fs :refer [->Config]])
nil
=> (->Config "resources/test/docs/"
             "resources/test/notes/"
             "resources/test/examples/")
#g/t [:grimoire.api.fs/Config
      {:docs     "resources/test/docs/",
       :notes    "resources/test/notes/",
       :examples "resources/test/examples/"}]
```

In the context of a configured Grimoire instance, the following would work:

#### Ex. 5

```Clojure
grimoire.web.views> (lib-grim-config)
#g/t [:grimoire.api.fs/Config
      {:docs "doc-store", :examples "notes-store", :notes "notes-store"}]

grimoire.web.views> (result (api/list-groups (lib-grim-config)))
(#g/t [:grimoire.things/group {:name "org.clojure", :grimoire.things/url "org.clojure"}]
 #g/t [:grimoire.things/group {:name "org.clojure-grimoire", :grimoire.things/url "org.clojure-grimoire"}])

grimoire.web.views> (result (api/list-artifacts (lib-grim-config) (second *1)))
(#g/t [:grimoire.things/artifact
       {:parent
        #g/t [:grimoire.things/group
              {:name "org.clojure-grimoire",
              :grimoire.things/url "org.clojure-grimoire"}],
        :name "lib-grimoire",
        :grimoire.things/url "org.clojure-grimoire/lib-grimoire"})])

grimoire.web.views> (result (api/list-versions (lib-grim-config) (first *1)))
(#g/t [:grimoire.things/version
       {:parent
        #g/t [:grimoire.things/artifact
              {:parent
               #g/t [:grimoire.things/group
                     {:name "org.clojure-grimoire",
                      :grimoire.things/url "org.clojure-grimoire"}],
               :name "lib-grimoire",
               :grimoire.things/url "org.clojure-grimoire/lib-grimoire"}],
               :name "0.6.4",
               :grimoire.things/url "org.clojure-grimoire/lib-grimoire/0.6.4"}]
      #g/t [:grimoire.things/version
            {:parent
             #g/t [:grimoire.things/artifact
                   {:parent
                     #g/t [:grimoire.things/group
                           {:name "org.clojure-grimoire",
                           :grimoire.things/url "org.clojure-grimoire"}],
                     :name "lib-grimoire",
                     :grimoire.things/url "org.clojure-grimoire/lib-grimoire"}],
             :name "0.6.3",
             :grimoire.things/url "org.clojure-grimoire/lib-grimoire/0.6.3"}])
```

Another cool feature of the Grimoire API is that it features a crude (and very alpha) searching engine.

The search datastructure DSL is as follows:

- Strings match Things with names equal to that string
- Regexes match Things with names matching that regex
- Sets match Things such that they contain the name of the Thing
- Functions match Things such that they return boolean truth on that Thing.
- Nil matches any Thing.

The query itself is formatted as a vector starting with one of

```clojure
#{:def :ns :platform :artifact :version :group}`
```

And having N terms where N is the number of URL path elements for a Thing of the corresponding type.

#### Ex. 6

```Cloure
;; Find the Clojure/Core group via search
grimoire.web.views> (api/search (lib-grim-config)
                                [:group "org.clojure"])
#g/t [:grimoire.either/Succeess {:result (#g/t [:grimoire.things/group {:name "org.clojure", :grimoire.things/url "org.clojure"}])}]

;; How many defs are in Clojure 1.6?
grimoire.web.views> (count (result (api/search (lib-grim-config) [:def nil nil "1.6.0" nil nil nil])))
921

;; How many defs are in the clj platform?
grimoire.web.views> (count (result (api/search (lib-grim-config) [:def nil nil nil "clj" nil nil])))
34758
;; this took a few seconds :P

;; How many defs are in alpha artifacts?
grimoire.web.views> (count (result (api/search (lib-grim-config) [:def nil nil #".*?alpha.*" nil nil nil])))
3734

;; How many different artifacts define "update"?
grimoire.web.views> (count (result (api/search (lib-grim-config) [:def nil nil nil nil nil "update"])))
18
```

### Grimoire back end

The [http API](http://conj.io/api) exposed by Grimoire is backed by an instance of lib-grimoire on the server side, so it only mades sense for me to dogfood the Grimoire datastore out over the same interface used internally.
The Grimoire back end (`grimoire.api.web.read` [docs](http://conj.io/store/v1/org.clojure-grimoire/lib-grimoire/latest/clj/grimoire.api.web.read)) provides full, read only access to the datastore behind Grimoire using EDN as the data interchange format and `clojure.edn/read-string` for the reader.
Lib-grimore does _not_ use a HTTP request client to implement this feature, instead relying only on `clojure.core/slurp` in the name of being lightweight.

This back end uses a configuration map as such:

#### Ex. 7

```Clojure
user> (require '[grimoire.api.web :refer [->Config]])
nil
user> (->Config "http://127.0.0.1:3000")
#g/t [:grimoire.api.fs/Config
 {:host "http://127.0.0.1:3000"}]
```

`:host` is expected to be the Grimoire base URL, but is variable and can be pointed anywhere.
Note that host string must include a protocol specifier, and must not end with `"/"`.

Rate limiting may be applied to this API on the server side in future in the form of `fail`ing requests.

So if you wanted to use the live Grimoire site as a data source for instance:

#### Ex. 8

```Clojure
user> (grimoire.api.web/->Config "http://conj.io")
#g/t [:grimoire.api.web/Config
      {:host "http://conj.io"}]
user> (api/list-groups *1)
#g/t [:grimoire.either/Succeess
      {:result
       (#g/t [:grimoire.things/group
              {:name "org.clojure",
               :grimoire.things/url "org.clojure"}]
        #g/t [:grimoire.things/group
              {:name "org.clojure-grimoire",
               :grimoire.things/url "org.clojure-grimoire"}])}]
```

If you want to forge links to Grimoire documentation, or for an API request against a Grimoire instance, then the functions `grimoire.api.web/make-html-url` and `grimoire.api.web/make-api-url` are your friends.

#### Ex. 9

```Clojure
user> (grimoire.api.web/->Config "http://conj.io")
#g/t [:grimoire.api.web/Config
      {:host "http://conj.io"}]

;; Forge a html link to the documentation of the org.clojure-grimoire group
user> (->> (grimoire.api/list-groups *1)
           (grimoire.either/result)
           first
           (grimoire.api.web/make-html-url *1))
"http://conj.io/store/v1/org.clojure-grimoire"

;; Forge an API request to the metadata of the org.clojure-grimoire group
user> (as-> (grimoire.api/list-groups *2) v
            (grimoire.either/result v)
            (first v)
            (grimoire.api.web/make-api-url *2 v "meta"))
"http://conj.io/api/v2/org.clojure-grimoire?op=meta&type=edn"
```

These two functions are the _only_ supported interface for generating links to Grimoire.
Future versions of Grimoire promise backwards compatibility for the URLs generated by old versions of these functions.
However URL scheme changes may occur at any time, and these functions are the only maintained URL forging implementations.
Note that they are heavily used within Grimoire as of 0.4.12 for exactly this reason.

## Changelog

**0.10.\***:
- Dropped the dependency on version-clj to resolve sorting issues with build qualifiers.
- Upgraded to guten-tag 0.1.4
- Deleted `grimoire.things/thing->relative-path`, depended on guten-tag < 1.3, not used.
- Deleted `grimoire.things/thing->root-to`, depended on guten-tag < 1.3, not used.
- Broke and fixed the version sorting order on `grimoire.api/list-versions`

**0.9.\***:
- Added `grimoire.things/thing->url-path` for cases when Things must be URL or path safe.
- Added `grimoire.things/url-path->thing` to complement `grimoire.things/thing->url-path`.
- **[BREAKING]** Switched to using augmented url encoding for munging in the filesystem, web backends.
  Note that the character '.' is encoded to '%2E', not to '.'.
  This is done to ensure that the symbols `clojure.core/.` and `clojure.core/..` map to path meaningful strings.
- `grimoire.things/->Example`, `grimoire.things/->Note` now take a "name" string as the 2nd argument.
- `grimoire.things/thing->name` no longer objects to an Example or Note instance.
- `grimoire.api.web/make-api-url` added.
- `grimoire.api.web/make-html-url` added.
- `grimoire.things/thing->type-name` added.
- `grimoire.things/thing->full-uri` added.
- `grimoire.things/full-uri->thing` added.
- `grimoire.things/thing->short-string` added.
- `grimoire.things/thing->url-path` refactored in terms of core.match.
- `grimoire.things` refactored to use `me.arrdem/guten-tag`.
- `grimoire.things/thing->path` now uses `grimoire.things/thing->url-path` under the hood.
- `grimoire.things` now uses [me.arrdem/guten-tag](http://github.com/arrdem/guten-tag) under the hood.
- `grimoire.either` now uses [me.arrdem/guten-tag](http://github.com/arrdem/guten-tag) under the hood.
- `grimoire.api/search` added.

**0.8.\***:
- Things are now encoded using Detritus' tagged values system.
- `Examples` have been added to the Things structure.
- `Notes` have been added to the Things structure.
- `grimoire.api/list-examples` added.
- `grimoire.api/list-notes` added.
- `grimoire.api/read-example` added.
- `grimoire.api/read-note` added.
- `grimoire.api/read-notes` reimplemented as the compose of `list-notes` and `read-note`.
- Refactor `grimoire.api` so that the intentional API consists of fns with the same names as the previous multimethod API guarding renamed multimethods with contract preconditions.
- Refactor `grimoire.api.fs.*` to make use of `grimoire.api.fs/->Config` (a Detritus tagged val) as it's configuration ctor and dispatch value.
- Refactor `grimoire.api.fs.*` to extend the renamed multimethods.
- Refactor `grimoire.api.web.*` to make use of `grimoire.api.web/->Config` (a Detritus tagged val) as it's configuration ctor and dispatch value.
- Refactor `grimoire.api.web.*` to extend the renamed multimethods.
- Refactor the various `grimoire.things/thing->T` parent traversals to provide assertion coverage guarding against nil results.
- Refactor the various `grimoire.things/T?` predicates to reflect the updated type graph structure.
- Fix logic inversion bug in `grimoire.api.fs.read`.
- Extend README significantly with examples & documentation.

**0.7.\***:
- The `:parent` of a `Namespace` is now a `Platform` not a `Version` as it was previously.
  This change allows for the Thing structure to losslessly encode namespaces and defs across both Clojure and ClojureScript.
  Note that [support is provided](/src/grimoire/util.clj:48) for Clojure, ClojureScript, Pixie, Oxlang and Toccata and that this set may be extended without breaking changes.

## Hacking

Note that the tests assume an instance of Grimoire 0.4 or later running on 127.0.0.1:3000.
Patches, PRs and issues welcome.
No CA required.

## License

Copyright © 2014-Present Reid "arrdem" McKenzie

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
