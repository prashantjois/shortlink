package ca.jois.shortlink.persistence

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey

@DynamoDbBean
data class DyShortLinkItem(
    @get:DynamoDbPartitionKey var code: String? = null,
    @get:DynamoDbSecondaryPartitionKey(indexNames = [Indexes.GSI.OWNER_INDEX])
    var owner: String? = null,
    var creator: String? = null,
    var expires_at: Long? = null,
    var url: String? = null,
    var created_at: Long? = null,
    @get:DynamoDbVersionAttribute var version: Long? = null
) {
    companion object {
        const val TABLE_NAME = "shortlinks"
        const val DELIMITER = "|||"
    }

    object Indexes {
        object GSI {
            const val OWNER_INDEX = "ownerIndex"
        }
    }
}
