# Shortlink JDBC Backend

Provides an implementation of [ShortlinkStore](../shortlink-lib/src/main/kotlin/persistence/ShortLinkStore.kt) that uses
JDBC to connect to the underlying database.

The [resources](src/main/resources) directory provides some sample schema for your tables.

## Usage

```kotlin
val storage = ShortLinkStoreInMemory()
```