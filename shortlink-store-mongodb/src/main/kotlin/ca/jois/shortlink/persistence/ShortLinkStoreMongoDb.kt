package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toDocument
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toShortLink
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.IndexOptions
import java.net.URL
import java.time.Clock
import org.bson.Document
import org.bson.conversions.Bson

/** An implementation of [ShortlinkStore] that uses MongoDB. */
class ShortLinkStoreMongoDb(
    connectionString: String,
    databaseName: String,
) : ShortLinkStore {
    private val client = MongoClients.create(connectionString)
    private val database = client.getDatabase(databaseName)
    private val shortLinksCollection = database.getCollection("shortlinks")

    init {
        shortLinksCollection.createIndex(
            Document(MongoDbFields.CODE.name, 1),
            IndexOptions().unique(true)
        )
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
                .find(Document(MongoDbFields.CODE.name, code.value))
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
                Document("\$set", Document(MongoDbFields.URL.name, url.toString()))
            )
        if (updateResult.matchedCount == 0L) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
    }

    override suspend fun update(updater: ShortLinkUser?, code: ShortCode, expiresAt: Long?) {
        val updateResult =
            shortLinksCollection.updateOne(
                modifyFilterWithOwner(updater, code),
                Document("\$set", Document(MongoDbFields.EXPIRES_AT.name, expiresAt))
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
                    Filters.and(
                        eq(MongoDbFields.CODE.name, code.value),
                        exists(MongoDbFields.OWNER.name, false)
                    )
                else ->
                    Filters.and(
                        eq(MongoDbFields.CODE.name, code.value),
                        Filters.or(
                            exists(MongoDbFields.OWNER.name, false),
                            eq(MongoDbFields.OWNER.name, user.identifier)
                        )
                    )
            }
        return filter
    }
}
