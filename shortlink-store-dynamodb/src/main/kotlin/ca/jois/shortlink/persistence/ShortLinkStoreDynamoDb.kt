package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.DynamoDbExtensions.groupOwnerKeyString
import ca.jois.shortlink.persistence.DynamoDbExtensions.partitionKeyString
import ca.jois.shortlink.persistence.DynamoDbExtensions.toDyShortLinkItem
import ca.jois.shortlink.persistence.DynamoDbExtensions.toKey
import ca.jois.shortlink.persistence.DynamoDbExtensions.toShortLink
import ca.jois.shortlink.persistence.ShortLinkStore.PaginatedResult
import ca.jois.shortlink.util.Encoding.fromBase64
import ca.jois.shortlink.util.Encoding.toBase64
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.net.URL
import java.time.Clock

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

  private val shortLinksTable =
    client.table(DyShortLinkItem.TABLE_NAME, TableSchema.fromBean(DyShortLinkItem::class.java))

  private val shortLinkGroupsTable =
    client.table(
      DyShortLinkGroupItem.TABLE_NAME,
      TableSchema.fromBean(DyShortLinkGroupItem::class.java),
    )

  override suspend fun listByGroupAndOwner(
    group: ShortLinkGroup,
    owner: ShortLinkUser,
    paginationKey: String?,
    limit: Int?,
  ): PaginatedResult<ShortLink> {
    val page = shortLinksTable
      .index(DyShortLinkItem.Indexes.GSI.GROUP_OWNER_INDEX)
      .query { builder ->
        builder.queryConditional(
          QueryConditional.keyEqualTo(groupOwnerKeyString(owner, group).toKey()),
        )
        paginationKey?.let {
          builder.exclusiveStartKey(
            ListByOwnerPaginationKey.decode(it).toExclusiveStartKey(),
          )
        }
        limit?.let { builder.limit(it) }
      }
      .firstOrNull() ?: return PaginatedResult(emptyList(), null)

    val shortLinks = page.items().map { it.toShortLink() }
    val nextPaginationKey =
      page.lastEvaluatedKey()?.let { ListByOwnerPaginationKey(it).encode() }
    return PaginatedResult(shortLinks, nextPaginationKey)
  }

  override suspend fun create(shortLink: ShortLink): ShortLink {
    shortLink.owner.identifier.let {
      require(!it.contains(DyShortLinkItem.DELIMITER)) { "Invalid owner identifier: $it" }
    }
    shortLinksTable.getItem(shortLink.partitionKeyString.toKey())?.let {
      throw ShortLinkStore.DuplicateShortCodeException(shortLink.group, shortLink.code)
    }
    shortLinksTable.putItem(shortLink.toDyShortLinkItem())
    return shortLink
  }

  context(Clock)
  override suspend fun get(
    code: ShortCode,
    group: ShortLinkGroup,
    excludeExpired: Boolean
  ): ShortLink? {
    val item = shortLinksTable.getItem(partitionKeyString(code, group).toKey()) ?: return null
    val shortLink = item.toShortLink()
    if (excludeExpired && shortLink.isExpired()) {
      return null
    }
    return shortLink
  }

  override suspend fun update(
    code: ShortCode,
    url: URL,
    group: ShortLinkGroup,
    updater: ShortLinkUser
  ) = update(code, group, updater) { it.copy(url = url) }

  override suspend fun update(
    code: ShortCode,
    expiresAt: Long?,
    group: ShortLinkGroup,
    updater: ShortLinkUser
  ) = update(code, group, updater) { it.copy(expiresAt = expiresAt) }

  private fun update(
    code: ShortCode,
    group: ShortLinkGroup,
    updater: ShortLinkUser?,
    update: (ShortLink) -> ShortLink
  ) {
    val existing =
      shortLinksTable.getItem(partitionKeyString(code, group).toKey())
        ?: throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)

    val shortLink = existing.toShortLink()
    if (shortLink.owner != ShortLinkUser.ANONYMOUS && shortLink.owner != updater) {
      throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
    }
    shortLinksTable.putItem(update(shortLink).toDyShortLinkItem(existing.version))
  }

  override suspend fun delete(code: ShortCode, group: ShortLinkGroup, deleter: ShortLinkUser) {
    val key = partitionKeyString(code, group).toKey()
    val item =
      shortLinksTable.getItem(key) ?: throw ShortLinkStore.NotFoundOrNotPermittedException(
        group,
        code,
      )
    val shortLink = item.toShortLink()
    if (shortLink.owner != ShortLinkUser.ANONYMOUS && shortLink.owner != deleter) {
      throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
    }
    shortLinksTable.deleteItem(key)
  }

  private fun getGroupId(group: ShortLinkGroup): ShortLinkGroup {
    return shortLinkGroupsTable.index(DyShortLinkGroupItem.Indexes.GSI.GROUP_NAME_INDEX)
      .query { builder ->
        builder.queryConditional(
          QueryConditional.keyEqualTo(group.name.toKey()),
        )
      }
      .firstOrNull()
      ?.items()
      ?.firstOrNull()
      ?.partition_key
      ?.let { ShortLinkGroup(it) }
      ?: throw GroupNotFoundException(group)
  }

  /**
   * Encapsulates the pagination key used to retrieve the next page of results from a paginated
   * query when listing short links by owner.
   */
  private data class ListByOwnerPaginationKey(
    val lastEvaluatedKey: MutableMap<String, AttributeValue>,
  ) {
    companion object {
      private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
      private val adapter = moshi.adapter(ListByOwnerPaginationKey::class.java)

      fun decode(encoded: String) = adapter.fromJson(encoded.fromBase64())!!
    }

    fun encode() = adapter.toJson(this).toBase64()

    fun toExclusiveStartKey(): Map<String, AttributeValue> {
      val keys =
        listOf(DyShortLinkItem::partition_key.name, DyShortLinkItem::group_owner.name)

      return keys.associateWith { AttributeValue.fromS(lastEvaluatedKey[it]!!.s()) }
    }
  }

  class GroupNotFoundException(group: ShortLinkGroup) :
    RuntimeException("Group not found: ${group.name}")
}
