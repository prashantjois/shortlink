package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.persistence.DynamoDbExtensions.toDyShortLinkItem
import ca.jois.shortlink.persistence.DynamoDbExtensions.toKey
import ca.jois.shortlink.persistence.DynamoDbExtensions.toShortLink
import java.net.URI
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput

@Testcontainers
class ShortLinkStoreDynamoDbTest : ShortLinkStoreTest {
    @Container
    val container: GenericContainer<*> =
        GenericContainer(DockerImageName.parse("amazon/dynamodb-local:latest"))
            .withExposedPorts(8000)
            .withCommand("-jar DynamoDBLocal.jar -inMemory -sharedDb")

    @BeforeEach
    fun setup() {
        createTable()
    }

    override val shortLinkStore: ShortLinkStore
        get() = ShortLinkStoreDynamoDb(dynamoDbClient())

    override suspend fun getDirect(code: ShortCode) = table().getItem(code.toKey())?.toShortLink()

    override suspend fun createDirect(shortLink: ShortLink): ShortLink {
        table().putItem(shortLink.toDyShortLinkItem())
        return shortLink
    }

    fun table(): DynamoDbTable<DyShortLinkItem> {
        val enhancedClient =
            DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient()).build()

        val table =
            enhancedClient.table(
                DyShortLinkItem.TABLE_NAME,
                TableSchema.fromBean(DyShortLinkItem::class.java)
            )
        return table
    }

    fun createTable() {
        table().createTable {
            it.provisionedThroughput(
                ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build()
            )
        }
    }

    fun dynamoDbClient(): DynamoDbClient {
        val endpoint = "http://${container.host}:${container.firstMappedPort}"
        return DynamoDbClient.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy"))
            )
            .region(Region.US_WEST_2) // The region is arbitrary since we're using a local DynamoDB
            .build()
    }
}
