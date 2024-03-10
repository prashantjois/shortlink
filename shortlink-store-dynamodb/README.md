# Shortlink MongoDB Backend

Provides an implementation of [ShortlinkStore](../shortlink-lib/src/main/kotlin/persistence/ShortLinkStore.kt) that uses
AWS DynamoDB.

## Usage

```kotlin
val client = DynamoDbClient.builder()
    .endpointOverride(URI.create("http://localhost:8000"))
    .region(Region.US_WEST_2)
    .credentialsProvider(
        StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy"))
    )
    .build()

val storage = ShortLinkStoreDynamoDb(client)
```
