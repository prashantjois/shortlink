package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import java.net.URL
import org.bson.Document

object ShortLinkMongoDbExtensions {

    fun ShortLink.toDocument() =
        Document(
            mapOf(
                MongoDbFields.CODE.name to code.value,
                MongoDbFields.URL.name to url.toString(),
                MongoDbFields.CREATED_AT.name to createdAt,
                MongoDbFields.EXPIRES_AT.name to expiresAt,
            )
        )

    fun Document.toShortLink() =
        ShortLink(
            code = ShortCode(getString(MongoDbFields.CODE.name)),
            url = URL(getString(MongoDbFields.URL.name)),
            createdAt = getLong(MongoDbFields.CREATED_AT.name),
            expiresAt = getLong(MongoDbFields.EXPIRES_AT.name),
        )
}
