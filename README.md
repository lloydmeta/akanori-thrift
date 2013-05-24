akanori-thrift
------------------

Scala Thrift project/tool for extracting Japanese phrases out of two files (one containing phrases from T2 and one containing phrases from T1, where T2 is later than T1, T2 and T1 being the same amount of time difference), analyse them using MeCab, store them in Redis, and rank them via Chi-Squared fitness test.

Compiling
=======

`$ sbt one-jar`

Usage
=====

```
Usage: TrendApp
          --file-older-expected path
          --file-older-observed path
          --file-newer-expected path
          --file-newer-observed path
          [--min-occurrence Int, defaults to 10]
          [--min-length Int, defaults to 1]
          [--max-length Int, defaults to 50]
          [--top Int, defaults to 50]
          [--drop-blacklisted Boolean, defaults to true]
          [--only-whitelisted Boolean, defaults to false]
          [--redis-host String, defaults to localhost]
          [--redis-db Int, defaults to 0]
          [--redis-port Int, defaults to 6379]
```

Example
```
$java -jar target/scala-2.10/akanori_2.10-1.0-one-jar.jar --file-newer-expected timeSliceData/post_content_2013-05-19-1200-1800.csv --file-newer-observed timeSliceData/post_content_2013-05-19-1800-2400.csv --file-older-expected timeSliceData/post_content_2013-05-18-1200-1800.csv --file-older-observed timeSliceData/post_content_2013-05-18-1800-2400.csv --drop-blacklisted true --only-whitelisted true --report-newer-chisquared true
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