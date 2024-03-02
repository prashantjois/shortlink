package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.DynamoDbExtensions.toDyShortLinkItem
import ca.jois.shortlink.persistence.DynamoDbExtensions.toKey
import ca.jois.shortlink.persistence.DynamoDbExtensions.toShortLink
import ca.jois.shortlink.persistence.ShortLinkStore.PaginatedResult
import ca.jois.shortlink.util.Encoding.fromBase64
import ca.jois.shortlink.util.Encoding.toBase64
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.net.URL
import java.time.Clock
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

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

    override suspend fun listByOwner(
        owner: ShortLinkUser,
        paginationKey: String?,
        limit: Int?,
    ): PaginatedResult<ShortLink> {
        val page =
            table
                .index(DyShortLinkItem.Indexes.GSI.OWNER_INDEX)
                .query { builder ->
                    builder.queryConditional(QueryConditional.keyEqualTo(owner.toKey()))
                    paginationKey?.let {
                        builder.exclusiveStartKey(
                            ListByOwnerPaginationKey.decode(it).toExclusiveStartKey()
                        )
                    }
                    limit?.let { builder.limit(it) }
                }
                .firstOrNull() ?: return PaginatedResult(emptyList(), null)

        val shortLinks = page.items().map { it.toShortLink() }
        val nextPaginationKey =
            page.lastEvaluatedKey()?.let {
                ListByOwnerPaginationKey(
                        it[DyShortLinkItem::code.name]!!.s(),
                        it[DyShortLinkItem::owner.name]!!.s()
                    )
                    .encode()
            }
        return PaginatedResult(shortLinks, nextPaginationKey)
    }

    override suspend fun create(shortLink: ShortLink): ShortLink {
        shortLink.owner.identifier.let {
            require(!it.contains(DyShortLinkItem.DELIMITER)) { "Invalid owner identifier: $it" }
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

    override suspend fun update(code: ShortCode, url: URL, updater: ShortLinkUser) =
        update(code, updater) { it.copy(url = url) }

    override suspend fun update(code: ShortCode, expiresAt: Long?, updater: ShortLinkUser) =
        update(code, updater) { it.copy(expiresAt = expiresAt) }

    private fun update(code: ShortCode, updater: ShortLinkUser?, update: (ShortLink) -> ShortLink) {
        val existing =
            table.getItem(code.toKey())
                ?: throw ShortLinkStore.NotFoundOrNotPermittedException(code)

        val shortLink = existing.toShortLink()
        if (shortLink.owner != ShortLinkUser.ANONYMOUS && shortLink.owner != updater) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
        table.putItem(update(shortLink).toDyShortLinkItem(existing.version))
    }

    override suspend fun delete(code: ShortCode, deleter: ShortLinkUser) {
        val item =
            table.getItem(code.toKey())
                ?: throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        val shortLink = item.toShortLink()
        if (shortLink.owner != ShortLinkUser.ANONYMOUS && shortLink.owner != deleter) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
        table.deleteItem(code.toKey())
    }
    /**
     * Encapsulates the pagination key used to retrieve the next page of results from a paginated
     * query when listing short links by owner.
     */
    private data class ListByOwnerPaginationKey(
        val code: String,
        val owner: String,
    ) {
        companion object {
            private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            private val adapter = moshi.adapter(ListByOwnerPaginationKey::class.java)

            fun decode(encoded: String) = adapter.fromJson(encoded.fromBase64())!!
        }

        fun encode() = adapter.toJson(this).toBase64()

        fun toExclusiveStartKey() =
            mapOf(
                DyShortLinkItem::code.name to AttributeValue.fromS(code),
                DyShortLinkItem::owner.name to AttributeValue.fromS(owner),
            )
    }
}
