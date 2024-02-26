package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toDocument
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toShortLink
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.exists
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
            Document(MongoDbFields.CODE.fieldName, 1),
            IndexOptions().unique(true)
        )
    }

    override suspend fun listByOwner(
        owner: ShortLinkUser,
        paginationKey: String?,
        limit: Int?,
    ): ShortLinkStore.PaginatedResult<ShortLink> {
        val limitOrDefault = limit ?: PAGE_SIZE
        val ownerFilter = eq(MongoDbFields.OWNER.fieldName, owner.identifier)
        val filter =
            paginationKey?.let { and(ownerFilter, gt(MongoDbFields.ID.fieldName, ObjectId(it))) }
                ?: ownerFilter

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
                throw ShortLinkStore.DuplicateShortCodeException(shortLink.code)
            }
            throw e
        }

        return shortLink
    }

    context(Clock)
    override suspend fun get(code: ShortCode, excludeExpired: Boolean): ShortLink? {
        val shortLink =
            shortLinksCollection
                .find(Document(MongoDbFields.CODE.fieldName, code.value))
                .firstOrNull()
                ?.toShortLink() ?: return null

        if (!excludeExpired || shortLink.doesNotExpire()) return shortLink

        return when (shortLink.isExpired()) {
            true -> null
            false -> shortLink
        }
    }

    override suspend fun update(updater: ShortLinkUser?, code: ShortCode, url: URL) {
        val updateResult =
            shortLinksCollection.updateOne(
                modifyFilterWithOwner(updater, code),
                Document("\$set", Document(MongoDbFields.URL.fieldName, url.toString()))
            )
        if (updateResult.matchedCount == 0L) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
    }

    override suspend fun update(updater: ShortLinkUser?, code: ShortCode, expiresAt: Long?) {
        val updateResult =
            shortLinksCollection.updateOne(
                modifyFilterWithOwner(updater, code),
                Document("\$set", Document(MongoDbFields.EXPIRES_AT.fieldName, expiresAt))
            )
        if (updateResult.matchedCount == 0L) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
    }

    override suspend fun delete(deleter: ShortLinkUser?, code: ShortCode) {
        val deleteResult =
            shortLinksCollection.deleteOne(
                modifyFilterWithOwner(deleter, code),
            )
        if (deleteResult.deletedCount == 0L) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
    }

    private fun modifyFilterWithOwner(user: ShortLinkUser?, code: ShortCode): Bson {
        val filter =
            when (user) {
                null ->
                    and(
                        eq(MongoDbFields.CODE.fieldName, code.value),
                        or(
                            eq(MongoDbFields.OWNER.fieldName, null),
                            exists(MongoDbFields.OWNER.fieldName, false)
                        )
                    )
                else ->
                    and(
                        eq(MongoDbFields.CODE.fieldName, code.value),
                        or(
                            eq(MongoDbFields.OWNER.fieldName, null),
                            exists(MongoDbFields.OWNER.fieldName, false),
                            eq(MongoDbFields.OWNER.fieldName, user.identifier)
                        )
                    )
            }
        return filter
    }
}
