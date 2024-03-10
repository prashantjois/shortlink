# Shortlink MongoDB Backend

Provides an implementation of [ShortlinkStore](../shortlink-lib/src/main/kotlin/persistence/ShortLinkStore.kt) that uses
MongoDB.

## Usage

```kotlin
val storage = ShortLinkStoreMongoDb(
    connectionString = "mongodb://127.0.0.1:27017/shortlinks",
    databaseName = "shortlinks",
)
```
