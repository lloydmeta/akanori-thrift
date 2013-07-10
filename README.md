akanori-thrift [![Build Status](https://travis-ci.org/lloydmeta/akanori-thrift.png?branch=master)](https://travis-ci.org/lloydmeta/akanori-thrift)
------------------

__Note__: Requires Java 7 because the [WatchService API](http://docs.oracle.com/javase/7/docs/api/java/nio/file/WatchService.html) in order to allow for on-the-fly updates of the dictionary used for tokenization.

Scala Thrift project/tool for extracting trends from Japanese phrases. Still in development.

Thrift clients send strings to the Scala server, which then stores it initially. Clients can then ask for trends at specific time slices in the past (granted the data for that timeslice), with certain parameters such as dropblacklisted, whitelisted only, etc, and the server will reply with a list of TrendResults by doing Chi-squared fitness tests and comparing results from the timeslice requested and the same timeslice 24 hours before.

Todo
===
* Make Storage access more abstract to enable the use of other non-Redis backends (interface trait?)
* More tests
* Less usage of futures with closures and move towards FSM ?

Compiling
=======

`$ sbt one-jar`

Usage
=====

```
Usage: Akanori-thrift (options are for currentTrendsDefault)
    --clear-redis Boolean
    [--thrift-server-port Int, defaults to 9090]
    [--span-in-seconds Int, defaults to 3 hours (10800)]
    [--min-occurrence Int, defaults to 10]
    [--min-length Int, defaults to 1]
    [--max-length Int, defaults to 50]
    [--top Int, defaults to 50]
    [--drop-blacklisted Boolean, defaults to true]
    [--only-whitelisted Boolean, defaults to false]
    [--redis-host String, defaults to localhost]
    [--redis-db Int, defaults to 0]
    [--redis-port Int, defaults to 6379]
    [--custom-dictionary-path String, path to a dictionary file (txt). See src/example/customDictionary.txt]
```

Example
```
$ java -jar target/scala-2.10/akanori-thrift_2.10-1.0-one-jar.jar --drop-blacklisted true --only-whitelisted true --min-occurrence 5 --top 50 --clear-redis false
```

## License

Copyright (c) 2013 by Lloyd Chan

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
