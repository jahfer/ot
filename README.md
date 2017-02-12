# ot [![Build Status](https://travis-ci.org/jahfer/ot.svg?branch=master)](https://travis-ci.org/jahfer/ot)
`ot` is a basic implementation of Operational Transforms, used to resolve concurrent edits of a document from multiple sources.

## Is this what I want?
Probably not. This is a first-pass at the idea. The library extracted from this is probably what you're interested in: [jahfer/othello](https://github.com/jahfer/othello). There's also an implementation using that library available at [jahfer/othello-editor](https://github.com/jahfer/othello-editor).

## Usage
### Start server:
```shell
$ lein server
```

### Start server using reloaded pattern:
```shell
$ lein repl
ot.repl> (go) # change code...
ot.repl> (reset)
```

### Compile and watch cljs + cljx:
```shell
$ lein client # runs tests on compile!
$ lein cljx auto
```

### Run tests:
```shell
$ lein cleantest
```

### Run specific group of tests:
```shell
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
