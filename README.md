# Lib-Grimoire

A small library for sharing code between
[grimoire](https://github.com/clojure-grimoire/grimoire) and
[lein-grim](https://github.com/clojure-grimoire/lein-grim) as well as
other project interested in interacting with the Grimoire
datastore. lib-grimoire privides a shared abstraction of a Clojure
Maven entity and a set of operations on various backends for reading
and writing data using these entities.

## Usage

Entities (`grimoire.things`
[docs](http://conj.io/store/clojure-grimoire/lib-grimoire/latest/grimoire.things))
are defined as follows:

```
Thing     ::= Sum[Group, Artifact, Version, Namespace, Def];
Group     ::= Record[                   Name: String];
Artifact  ::= Record[Parent: Group,     Name: String];
Version   ::= Record[Parent: Artifact,  Name: String];
Namespace ::= Record[Parent: Version,   Name: String];
Def       ::= Record[Parent: Namespace, Name: String];
```

The Grimoire API (`grimoire.api`) is simply a collection of
multimethods, predominantly of the argument structure
`[configuration, target-thing]`. Several implementations of these
multimethods are provided, however they are not loaded by default. API
clients are responsible for loading what portions of the API they wish
to use.

The entire API is written in terms of `Either`, encoded using
`grimoire.util/succeed`, `grimoire.util/fail`,
`grimoire.util/succeed?` and `grimoire.util/result`. Exceptions should
be handled, please report encountered exceptions as bugs.

### FS backend

This is backend (`grimoire.api.fs.read`, `grimoire.api.fs.write`)
implements reading and writing on a filesystem datastore as used in
Grimoire 0.4 and generated by lib-grimoire. Load the reader and writer
as desired and then use the API exposed in `grimoire.api` as needed.

This backend uses a configuration map as such:

```Clojure
{:datastore
 {:docs  "resources/test/docs/",
  :notes "resources/test/notes/",
  :mode  :filesystem}}
```

The `:mode :filesystem` is required if you want to use the FS
backend. The `:docs` key should be a path to a directory where you
want to read / write documentation. `:notes` should be a path to a
directory where you want to read / write notes. Note that they need
not be equal.

At present, while reading examples is supported, writing examples via
the FS backend is not supported.

### Grimoire backend

The [http API](http://conj.io/api) exposed by Grimoire is backed by an
instance of lib-grimoire on the server side, so it only mades sense
for me to dogfood the Grimoire datastore out over the same interface
used internally. The Grimoire backend (`grimoire.api.web.read`)
provides full, read only access to the datastore behind Grimoire using
EDN as the data interchange format and `clojure.edn/read-string` for
the reader. Lib-grimore does _not_ use a HTTP request client to
implement this feature, instead relying only on `clojure.core/slurp`
in the name of being lightweight.

This backend uses a configuration map as such:

```Clojure
{:datastore
 {:mode :web
  :host "http://conj.io"}}
```

As with the filesystem backend, the `:mode` key is used to select the
implementation and must be set to `:web` in order to use the Grimoire
REST target. `:host` is expected to be the Grimoire base URL, but is
variable and can be pointed anywhere. Note that host string must
include a protocol specifier, and must not end with `"/"`.

Rate limiting may be applied to this API on the server side in future
in the form of `Fail`ing requests.

## License

Copyright © 2014 Reid "arrdem" McKenzie

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
