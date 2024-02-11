package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.DynamoDbExtensions.toDyShortLinkItem
import ca.jois.shortlink.persistence.DynamoDbExtensions.toKey
import ca.jois.shortlink.persistence.DynamoDbExtensions.toShortLink
import java.net.URL
import java.time.Clock
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

/**
 * A [ShortLinkStore] implementation that uses Amazon DynamoDB as the underlying storage.
 *
 * @param dynamoDbClient The [DynamoDbClient] instance to use for interacting with the DynamoDB
 *   service.
 *
 * Usage:
 * ```
 *  val credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy"))
 *  val client = DynamoDbClient.builder()
 *   .endpointOverride(URI.create("http://localhost:8000"))
 *   .region(Region.US_WEST_2)
 *   .credentialsProvider(credentials)
 *   .build()
 *
 *   val shortLinkStore = ShortLinkStoreDynamoDb(client)
 * ```
 */
class ShortLinkStoreDynamoDb(dynamoDbClient: DynamoDbClient) : ShortLinkStore {
    private val client = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build()

    private val table =
        client.table(DyShortLinkItem.TABLE_NAME, TableSchema.fromBean(DyShortLinkItem::class.java))

    override suspend fun create(shortLink: ShortLink): ShortLink {
        shortLink.owner?.identifier?.let {
            require(!it.contains(DyShortLinkItem.DELIMITER)) { "Invalid owner identifier: $it" }
            require(!it.contains(DyShortLinkItem.NO_USER)) { "Invalid owner identifier: $it" }
        }
        table.getItem(shortLink.code.toKey())?.let {
            throw ShortLinkStore.DuplicateShortCodeException(shortLink.code)
        }
        table.putItem(shortLink.toDyShortLinkItem())
        return shortLink
    }

    context(Clock)
    override suspend fun get(code: ShortCode, excludeExpired: Boolean): ShortLink? {
        val item = table.getItem(code.toKey()) ?: return null
        val shortLink = item.toShortLink()
        if (excludeExpired && shortLink.isExpired()) {
            return null
        }
        return shortLink
    }

    override suspend fun update(updater: ShortLinkUser?, code: ShortCode, url: URL) =
        update(updater, code) { it.copy(url = url) }

    override suspend fun update(updater: ShortLinkUser?, code: ShortCode, expiresAt: Long?) =
        update(updater, code) { it.copy(expiresAt = expiresAt) }

    private fun update(updater: ShortLinkUser?, code: ShortCode, update: (ShortLink) -> ShortLink) {
        val existing =
            table.getItem(code.toKey())
                ?: throw ShortLinkStore.NotFoundOrNotPermittedException(code)

        val shortLink = existing.toShortLink()
        if (shortLink.owner != null && shortLink.owner != updater) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
        table.putItem(update(shortLink).toDyShortLinkItem(existing.version))
    }

    override suspend fun delete(deleter: ShortLinkUser?, code: ShortCode) {
        val item =
            table.getItem(code.toKey())
                ?: throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        val shortLink = item.toShortLink()
        if (shortLink.owner != null && shortLink.owner != deleter) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
        table.deleteItem(code.toKey())
    }
}
