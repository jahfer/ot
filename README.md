# ot [![Build Status](https://travis-ci.org/jahfer/ot.svg?branch=master)](https://travis-ci.org/jahfer/ot)
`ot` is a basic implementation of Operational Transforms, used to resolve concurrent edits of a document from multiple sources.

## Usage
```
Start server:
$ lein server

Start server using reloaded pattern:
$ lein repl
$ (go) # change code...
$ (reset)

Compile and watch cljs + cljx:
$ lein client # runs tests on compile!
$ lein cljx auto

Run tests:
$ lein cleantest

Run specific group of tests:
$ lein do cljx, clj-test # or...
$ lein do cljx, cljs-test
```

## References
- [Understanding and Applying Operational Transformation](http://www.codecommit.com/blog/java/understanding-and-applying-operational-transformation)
- [Operational Transform on Wikipedia](http://en.wikipedia.org/wiki/Operational_transformation)
- [OT FAQ](http://cooffice.ntu.edu.sg/otfaq/)
- [Google Wave Operational Transformation](http://www.waveprotocol.org/whitepapers/operational-transform)
- [OT Explained](http://operational-transformation.github.io/index.html)

## License
Copyright © 2015 EPL

Distributed under the Eclipse Public License version 1.0.
