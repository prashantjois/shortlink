# shortlink

A simple shortlink generation app.

## Development Environment

Add a pre-commit hook for code style:

```bash
.hooks/install
```

## Development Server

See the [shortlink-app module](shortlink-app/README.md)

## Shortlink Persistence Backend

You can use a variety of backend systems for persistence, or implement your own.

Existing implementations:

* [In-memory Store](shortlink-store-in-memory/README.md)
* [JDBC Store](shortlink-store-jdbc/README.md)
* [MongoDB](shortlink-store-mongodb/README.md)

Custom implementations:

* Implement the [ShortLinkStore](shortlink-lib/src/main/kotlin/persistence/ShortLinkStore.kt) interface and pass it to
  an implementation of [ShortlinkManager](shortlink-lib/src/main/kotlin/manager/ShortLinkManager.kt) (such
  as [RealShortLinkManager](shortlink-lib/src/main/kotlin/manager/RealShortLinkManager.kt))