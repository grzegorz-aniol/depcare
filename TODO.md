### TODO

1. Sometimes dependency specified in the POM may not indicate the version directly. In order to determine the correct version we should check the parent POM.
2. Store exclusions for dependency
3. Resolve properties into version numbers (property may be defined in current POM or in parent POM!)

4. Issue with groupId from spring repository

{"fileSize":4,"publishedAt":"2020-03-26T12:07:00","groupId":"libs-rele│{"publishedAt":"2020-03-26T12:45:00","fileSize":403,"groupId":"org.spr│
│ase.org.springframework.boot","artifactId":"spring-boot-starter-web","│ingframework.boot","artifactId":"spring-boot-starter-web","version":"2│
│version":"2.2.6.RELEASE","url":"https://repo.spring.io/artifactory/lib│.2.6.RELEASE","url":"https://repo1.maven.org/maven2/org/springframewor│
│s-release/org/springframework/boot/spring-boot-starter-web/2.2.6.RELEA│k/boot/spring-boot-starter-web/2.2.6.RELEASE/"}                       │
│SE/"}                                                                 │                                                    


match (v1:Version)
where v1.groupId starts with 'libs-release'
match (v2:Version)
where ID(v1)<>ID(v2) and v1.artifactId=v2.artifactId and v1.version=v2.version and v1.groupId ends with v2.groupId
return v1, v2
limit 10

(a) Duplicated Groups should be merged
(b) Duplicated Versions should be merged
(c) URL and Published At should be stored as map
(d) file size is wrong (?)


