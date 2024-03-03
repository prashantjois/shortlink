package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL
import org.bson.Document

object ShortLinkMongoDbExtensions {
    fun ShortLink.toDocument() =
        Document(
            mapOf(
                MongoDbFields.CODE.fieldName to code.value,
                MongoDbFields.URL.fieldName to url.toString(),
                MongoDbFields.CREATED_AT.fieldName to createdAt,
                MongoDbFields.EXPIRES_AT.fieldName to expiresAt,
                MongoDbFields.OWNER.fieldName to owner.identifier,
                MongoDbFields.CREATOR.fieldName to creator.identifier,
                MongoDbFields.GROUP.fieldName to group.name,
            )
        )

    fun Document.toShortLink() =
        ShortLink(
            code = ShortCode(getString(MongoDbFields.CODE.fieldName)),
            url = URL(getString(MongoDbFields.URL.fieldName)),
            createdAt = getLong(MongoDbFields.CREATED_AT.fieldName),
            expiresAt = getLong(MongoDbFields.EXPIRES_AT.fieldName),
            owner = ShortLinkUser(getString(MongoDbFields.OWNER.fieldName)!!),
            creator = ShortLinkUser(getString(MongoDbFields.CREATOR.fieldName)!!),
            group = ShortLinkGroup(getString(MongoDbFields.GROUP.fieldName)!!),
        )
}
