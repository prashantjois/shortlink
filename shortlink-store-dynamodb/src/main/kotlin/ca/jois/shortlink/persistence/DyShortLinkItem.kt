package ca.jois.shortlink.persistence

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class DyShortLinkItem(
    @get:DynamoDbPartitionKey var code: String? = null,
    var expires_at: Long? = null,
    var url: String? = null,
    var created_at: Long? = null,
    var owner: String? = null,
    var creator: String? = null,
    @get:DynamoDbVersionAttribute var version: Long? = null
) {
    companion object {
        val TABLE_NAME = "shortlinks"
        const val NO_USER = "NU"
        const val DELIMITER = "|||"
    }
}
