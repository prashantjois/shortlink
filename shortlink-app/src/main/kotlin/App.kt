package shortlinkapp

import ca.jois.shortlink.persistence.DyShortLinkItem
import ca.jois.shortlink.persistence.ShortLinkStoreDynamoDb
import ca.jois.shortlink.persistence.ShortLinkStoreInMemory
import ca.jois.shortlink.persistence.ShortLinkStoreJdbc
import ca.jois.shortlink.persistence.ShortLinkStoreMongoDb
import java.net.URI
import shortlinkapp.api.service.WebServer
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException

fun main() {
    // Uncomment to use in-memory storage
    //  val shortLinkStore = inMem()

    // Uncomment to use MySQL database server
    val shortLinkStore = jdbc()

    // Uncomment to use MongoDB database server
    // val shortLinkStore = mongo()

    // Uncomment to use DynamoDB database server
    //      val shortLinkStore = dynamodb()

    WebServer(port = 8080, shortLinkStore = shortLinkStore).run()
}

private fun inMem() = ShortLinkStoreInMemory()

private fun jdbc() =
    ShortLinkStoreJdbc.configure {
        jdbcUrl = "jdbc:mysql://127.0.0.1:3306/shortlinks"
        username = "root"
    }

private fun mongo() =
    ShortLinkStoreMongoDb(
        connectionString = "mongodb://127.0.0.1:27017/shortlinks",
        databaseName = "shortlinks",
    )

private fun dynamodb(): ShortLinkStoreDynamoDb {
    val client =
        DynamoDbClient.builder()
            .endpointOverride(URI.create("http://localhost:8000"))
            .region(Region.US_WEST_2)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy"))
            )
            .build()
    val enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()

    val table =
        enhancedClient.table(
            DyShortLinkItem.TABLE_NAME,
            TableSchema.fromBean(DyShortLinkItem::class.java)
        )

    try {
        table.createTable {
            it.provisionedThroughput(
                ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build()
            )
        }
    } catch (e: ResourceInUseException) {
        if (e.message?.contains("Cannot create preexisting table") == true) {
            println("Table already exists")
        } else {
            throw e
        }
    }

    return ShortLinkStoreDynamoDb(client)
}
