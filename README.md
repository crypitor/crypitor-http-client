# crypitor-http-client
High performance http-client

# Guide
1. Add repository
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

2. Add dependency
```xml
	<dependency>
	    <groupId>com.github.crypitor</groupId>
	    <artifactId>crypitor-http-client</artifactId>
	    <version>v1.0</version>
	</dependency>
```
3. Using
```java
ResponseObject responseObject = HttpHandler.getInstance().doGet(ResponseObject.class, "localhost", CustomHttpHeaders.custom().build());
```
