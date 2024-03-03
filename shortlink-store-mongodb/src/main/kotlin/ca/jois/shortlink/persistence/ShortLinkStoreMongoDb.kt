package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toDocument
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toShortLink
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.IndexOptions
import java.net.URL
import java.time.Clock
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId

/** An implementation of [ShortLinkStore] that uses MongoDB. */
class ShortLinkStoreMongoDb(
    connectionString: String,
    databaseName: String,
) : ShortLinkStore {
    companion object {
        private const val PAGE_SIZE = 100
        private const val SHORT_LINK_STORE_COLLECTION = "shortlinks"
    }

    private val client = MongoClients.create(connectionString)
    private val database = client.getDatabase(databaseName)
    private val shortLinksCollection = database.getCollection(SHORT_LINK_STORE_COLLECTION)

    init {
        shortLinksCollection.createIndex(
            Document(
                mapOf(
                    MongoDbFields.GROUP.fieldName to 1,
                    MongoDbFields.CODE.fieldName to 1,
                ),
            ),
            IndexOptions().unique(true)
        )
    }

    override suspend fun listByOwner(
        group: ShortLinkGroup,
        owner: ShortLinkUser,
        paginationKey: String?,
        limit: Int?,
    ): ShortLinkStore.PaginatedResult<ShortLink> {
        val limitOrDefault = limit ?: PAGE_SIZE
        val ownerFilter = eq(MongoDbFields.OWNER.fieldName, owner.identifier)
        val groupFilter = eq(MongoDbFields.GROUP.fieldName, group.name)
        val filter =
            paginationKey?.let {
                and(
                    ownerFilter,
                    groupFilter,
                    gt(MongoDbFields.ID.fieldName, ObjectId(it)),
                )
            } ?: and(ownerFilter, groupFilter)

        val results = shortLinksCollection.find(filter).limit(limitOrDefault)
        val shortLinks = results.map { it.toShortLink() }.toList()
        val nextId =
            when (shortLinks.size < limitOrDefault) {
                true -> null
                false -> {
                    results.lastOrNull()?.getObjectId(MongoDbFields.ID.fieldName)?.toString()
                }
            }
        return ShortLinkStore.PaginatedResult(shortLinks, nextId)
    }

    override suspend fun create(shortLink: ShortLink): ShortLink {
        try {
            shortLinksCollection.insertOne(shortLink.toDocument())
        } catch (e: MongoWriteException) {
            if (e.message?.contains("duplicate key error") == true) {
                throw ShortLinkStore.DuplicateShortCodeException(shortLink.group, shortLink.code)
            }
            throw e
        }

        return shortLink
    }

    context(Clock)
    override suspend fun get(
        code: ShortCode,
        group: ShortLinkGroup,
        excludeExpired: Boolean
    ): ShortLink? {
        val shortLink =
            shortLinksCollection
                .find(Document(MongoDbFields.CODE.fieldName, code.value))
                .map { it.toShortLink() }
                .firstOrNull { it.group == group } ?: return null

        if (!excludeExpired || shortLink.doesNotExpire()) return shortLink

        return when (shortLink.isExpired()) {
            true -> null
            false -> shortLink
        }
    }

    override suspend fun update(
        code: ShortCode,
        url: URL,
        group: ShortLinkGroup,
        updater: ShortLinkUser
    ) {
        val updateResult =
            shortLinksCollection.updateOne(
                modifyFilterWithOwner(code, group, updater),
                Document("\$set", Document(MongoDbFields.URL.fieldName, url.toString()))
            )
        if (updateResult.matchedCount == 0L) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
        }
    }

    override suspend fun update(
        code: ShortCode,
        expiresAt: Long?,
        group: ShortLinkGroup,
        updater: ShortLinkUser
    ) {
        val updateResult =
            shortLinksCollection.updateOne(
                modifyFilterWithOwner(code, group, updater),
                Document("\$set", Document(MongoDbFields.EXPIRES_AT.fieldName, expiresAt))
            )
        if (updateResult.matchedCount == 0L) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
        }
    }

    override suspend fun delete(code: ShortCode, group: ShortLinkGroup, deleter: ShortLinkUser) {
        val deleteResult =
            shortLinksCollection.deleteOne(
                modifyFilterWithOwner(code, group, deleter),
            )
        if (deleteResult.deletedCount == 0L) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
        }
    }

    private fun modifyFilterWithOwner(
        code: ShortCode,
        group: ShortLinkGroup,
        user: ShortLinkUser
    ): Bson {
        return and(
            eq(MongoDbFields.CODE.fieldName, code.value),
            eq(MongoDbFields.GROUP.fieldName, group.name),
            or(
                eq(MongoDbFields.OWNER.fieldName, ShortLinkUser.ANONYMOUS.identifier),
                eq(MongoDbFields.OWNER.fieldName, user.identifier)
            )
        )
    }
}
