# depcare

## Repositories
* Spring repository:  `https://repo.spring.io/artifactory/libs-release/`
* Maven repository: `https://repo1.maven.org/maven2/`

Fetching page in HTML:
```
curl -L https://repo.spring.io/artifactory/libs-release/io -H "Accept: application/xml, */*" -X GET
```

Result:
```text
curl -L https://repo.spring.io/artifactory/libs-release/org/apache -H "Content-Type: text/xml" -X GET 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
<html>
<head><meta name="robots" content="noindex" />
<title>Index of libs-release/org/apache</title>
</head>
<body>
<h1>Index of libs-release/org/apache</h1>
<pre>Name       Last modified      Size</pre><hr/>
<pre><a href="../">../</a>
<a href="accumulo/">accumulo/</a>   03-Apr-2015 13:27    -
<a href="avro/">avro/</a>       03-Oct-2013 14:57    -
<a href="calcite/">calcite/</a>    03-Apr-2015 13:27    -
<a href="flume/">flume/</a>      03-Apr-2015 13:27    -
<a href="geode/">geode/</a>      15-Oct-2019 14:57    -
<a href="hadoop/">hadoop/</a>     11-Jul-2013 01:02    -
<a href="hbase/">hbase/</a>      11-Jul-2013 01:03    -
<a href="hcatalog/">hcatalog/</a>   08-Nov-2013 16:12    -
<a href="hive/">hive/</a>       11-Jul-2013 01:03    -
<a href="knox/">knox/</a>       03-Apr-2015 13:29    -
<a href="oozie/">oozie/</a>      03-Apr-2015 14:11    -
<a href="pig/">pig/</a>        11-Jul-2013 01:03    -
<a href="ranger/">ranger/</a>     03-Apr-2015 13:30    -
<a href="spark/">spark/</a>      03-Apr-2015 13:30    -
<a href="sqoop/">sqoop/</a>      08-Nov-2013 16:12    -
<a href="tez/">tez/</a>        03-Apr-2015 13:30    -
<a href="thrift/">thrift/</a>     05-May-2013 13:56    -
<a href="zookeeper/">zookeeper/</a>  11-Jul-2013 01:03    -
</pre>
<hr/><address style="font-size:small;">Artifactory Online Server at repo.spring.io Port 80</address></body></html>% 
```

Fetch module versions
```text
curl -L https://repo1.maven.org/maven2/org/apache/commons 
```

Result
```text
<a href="../">../</a>
<a href="3.0/" title="3.0/">3.0/</a>                                           2011-07-19 03:36         -      
<a href="3.0.1/" title="3.0.1/">3.0.1/</a>                                     2011-08-10 03:44         -      
<a href="3.1/" title="3.1/">3.1/</a>                                           2011-11-15 07:27         -      
<a href="3.10/" title="3.10/">3.10/</a>                                        2020-03-23 13:39         -      
<a href="3.11/" title="3.11/">3.11/</a>                                        2020-07-12 13:32         -      
<a href="3.12.0/" title="3.12.0/">3.12.0/</a>                                  2021-02-26 20:40         -      
<a href="3.2/" title="3.2/">3.2/</a>                                           2013-12-28 17:06         -      
<a href="3.2.1/" title="3.2.1/">3.2.1/</a>                                     2014-01-05 17:02         -      
<a href="3.3/" title="3.3/">3.3/</a>                                           2014-02-28 09:08         -      
<a href="3.3.1/" title="3.3.1/">3.3.1/</a>                                     2014-03-15 13:04         -      
<a href="3.3.2/" title="3.3.2/">3.3.2/</a>                                     2014-04-06 12:21         -      
<a href="3.4/" title="3.4/">3.4/</a>                                           2015-04-03 12:31         -      
<a href="3.5/" title="3.5/">3.5/</a>                                           2016-10-13 19:53         -      
<a href="3.6/" title="3.6/">3.6/</a>                                           2017-06-09 09:41         -      
<a href="3.7/" title="3.7/">3.7/</a>                                           2017-11-04 18:16         -      
<a href="3.8/" title="3.8/">3.8/</a>                                           2018-08-16 01:37         -      
<a href="3.8.1/" title="3.8.1/">3.8.1/</a>                                     2018-09-19 15:24         -      
<a href="3.9/" title="3.9/">3.9/</a>                                           2019-04-11 01:30         -      
<a href="maven-metadata-local.xml" title="maven-metadata-local.xml">maven-metadata-local.xml</a>                          2021-03-01 21:40       305      
<a href="maven-metadata-local.xml.md5" title="maven-metadata-local.xml.md5">maven-metadata-local.xml.md5</a>                      2021-03-01 21:40        32      
<a href="maven-metadata-local.xml.sha1" title="maven-metadata-local.xml.sha1">maven-metadata-local.xml.sha1</a>                     2021-03-01 21:40        40      
<a href="maven-metadata.xml" title="maven-metadata.xml">maven-metadata.xml</a>                                2021-03-01 21:40       846      
<a href="maven-metadata.xml.md5" title="maven-metadata.xml.md5">maven-metadata.xml.md5</a>                            2021-03-01 21:40        32      
<a href="maven-metadata.xml.sha1" title="maven-metadata.xml.sha1">maven-metadata.xml.sha1</a>                           2021-03-01 21:40        40      
<a href="maven-metadata.xml.sha256" title="maven-metadata.xml.sha256">maven-metadata.xml.sha256</a>                         2021-03-01 21:40        64      
<a href="maven-metadata.xml.sha512" title="maven-metadata.xml.sha512">maven-metadata.xml.sha512</a>                         2021-03-01 21:40       128 
```

## Pushing URL seed
```text
curl -v http://localhost:8080/api/repo/url -H "Content-Type: text/plain" -X POST -d 'https://repo.spring.io/artifactory/libs-release/org'
```
