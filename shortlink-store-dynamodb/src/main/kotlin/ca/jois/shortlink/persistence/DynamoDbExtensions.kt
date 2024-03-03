package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL
import software.amazon.awssdk.enhanced.dynamodb.Key

object DynamoDbExtensions {
    fun String.toKey(): Key = Key.builder().partitionValue(this).build()

    fun ShortCode.toKey(): Key = value.toKey()

    fun ShortLinkUser.toKey(): Key = identifier.toKey()

    fun ShortLink.toDyShortLinkItem(version: Long? = null) =
        DyShortLinkItem(
            partition_key = partitionKeyString,
            group_owner = groupOwnerString,
            code = code.value,
            group = group.name,
            created_at = createdAt,
            expires_at = expiresAt ?: Long.MAX_VALUE,
            url = url.toString(),
            owner = owner.identifier,
            creator = creator.identifier,
            version = version,
        )

    fun DyShortLinkItem.toShortLink(): ShortLink {
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
            creator = ShortLinkUser(creator!!),
            owner = ShortLinkUser(owner!!),
            group = ShortLinkGroup(group!!),
        )
    }

    fun partitionKeyString(code: ShortCode, group: ShortLinkGroup): String {
        return listOf(group.name, code.value).joinToString(DyShortLinkItem.DELIMITER)
    }

    fun groupOwnerString(owner: ShortLinkUser, group: ShortLinkGroup): String {
        return listOf(group.name, owner.identifier).joinToString(DyShortLinkItem.DELIMITER)
    }

    val ShortLink.partitionKeyString: String
        get() {
            return partitionKeyString(code, group)
        }

    val ShortLink.groupOwnerString: String
        get() {
            return groupOwnerString(owner, group)
        }
}
