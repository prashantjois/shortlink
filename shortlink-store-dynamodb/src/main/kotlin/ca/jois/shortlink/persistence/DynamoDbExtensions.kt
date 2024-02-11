package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL
import software.amazon.awssdk.enhanced.dynamodb.Key

object DynamoDbExtensions {
    fun ShortCode.toKey() = Key.builder().partitionValue(value).build()

    fun ShortLink.toDyShortLinkItem(version: Long? = null) =
        DyShortLinkItem(
            code = code.value,
            created_at = createdAt,
            expires_at = expiresAt ?: Long.MAX_VALUE,
            url = url.toString(),
            owner = owner?.identifier,
            creator = creator?.identifier,
            version = version,
        )

    fun DyShortLinkItem.toShortLink(): ShortLink {
        val ownerIdentifier =
            when (owner) {
                DyShortLinkItem.NO_USER -> null
                else -> owner
            }

        val expiresAt =
            when (expires_at) {
                Long.MAX_VALUE -> null
                else -> expires_at
            }

        return ShortLink(
            code = ShortCode(code!!),
            url = URL(url),
            createdAt = created_at!!,
            expiresAt = expiresAt,
            creator = creator?.let { ShortLinkUser(it) },
            owner = ownerIdentifier?.let { ShortLinkUser(it) },
        )
    }
}
