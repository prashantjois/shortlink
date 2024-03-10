# ShortLink

Shortlink is a flexible and extensible library for creating and managing shortened URLs. It provides a comprehensive set
of interfaces to generate short codes, manage short link lifecycles, and persist short links using various backends.

## Components

### Model

`ShortLink` is the core data model in the library. It includes:

* `code`: A unique identifier for the shortened URL.
* `url`: The original URL being shortened.
* `createdAt`: The timestamp when the short link was created.
* `expiresAt`: (Optional) The timestamp when the short link expires
* `creator`: (Optional) The user who created the short link.
* `owner`: (Optional) The user who is allowed to modify the short link.

### Generator

`ShortCodeGenerator` is the interface responsible for generating unique codes. These codes are what are used to map to
the URL you are shortening.

### Storage

Implementations of the `ShortLinkStore` interface specify how short links are persisted. The library currently supports
these types of storage:

* `In-Memory`: For lightweight or testing purposes.
* `JDBC`: For relational databases, using Java Database Connectivity.
* `MongoDB`: For those preferring a NoSQL database.
* `DynamoDB`: For AWS DynamoDB.

### Manager

The `ShortLinkManager` interface is the primary way to interact with the shortlink library. It uses
a `ShortCodeGenerator` to generate a short code and a `ShortLinkStore` to persist the short link. It provides methods to
create, read, update, and delete short links.

## Getting Started

To get started with shortlink, include it in your project's dependencies. Here's how you might do it with Maven:

Maven:

```xml

<dependency>
    <groupId>your.groupId</groupId>
    <artifactId>shortlink</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'your.groupId:shortlink:1.0.0'
```

```kotlin
implementation("your.groupId:shortlink:1.0.0")
```

## Usage

```kotlin
/* Initialize your manager */
val storage = ShortLinkStoreJdbc.configure {
  jdbcUrl = "jdbc:mysql://127.0.0.1:3306/shortlinks"
  username = "username"
  password = "password"
}
val generator = NaiveShortCodeGenerator()
val manager = RealShortLinkManager(storage, generator)

/* Shorten a new URL */
val longUrl = "https://www.example.com/products/category/subcategory/item?color=blue&size=medium&sort=popular&newArrivals=true"
val expiresAt = System.currentTimeMillis()
val shortLink = manager.create(ShortLinkUser("username"), URL(longUrl), expiresAt).also {
  println(it.code) // The unique code generated to 
}

/* Oops, we made a mistake, update the URL the code points to */
val fixedUrl = "https://www.example.com/products/category/subcategory/item?color=red&size=medium&sort=popular&newArrivals=true"
manager.update(ShortLinkUser("username"), shortLink.code, URL(fixedUrl)).also {
  println(it.code) // This will be the same from when we created it
  println(it.url) // The URL will be updated given the value we passed in
}

/* Turns out lots of people are using the code, we don't want to expire */
manager.update(ShortLinkUser("username"), shortLink.code, null).also {
  println(it.code) // This will be the same from when we created it
  println(it.expiresAt) // null because the link does not expire
}

/* We have regrets, let's get rid of the entry altogether */
manager.delete(ShortLinkUser("username"), shortLink.code)
```

### Web Server

A simple web server is provided to illustrate how you could build HTTP APIs against
Run the `main()` function in `App.kt` in the [shortlink-app](shortlink-app/README.md) module.
