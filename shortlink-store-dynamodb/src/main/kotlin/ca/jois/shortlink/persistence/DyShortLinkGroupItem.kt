package ca.jois.shortlink.persistence

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey

@DynamoDbBean
data class DyShortLinkGroupItem(
  @get:DynamoDbPartitionKey var partition_key: String? = null,
  @get:DynamoDbSecondaryPartitionKey(indexNames = [Indexes.GSI.GROUP_NAME_INDEX])
  var name: String? = null,
  @get:DynamoDbVersionAttribute var version: Long? = null
) {
  companion object {
    const val TABLE_NAME = "shortlink_groups"
  }

  object Indexes {
    object GSI {
      const val GROUP_NAME_INDEX = "groupNameIndex"
    }
  }
}
