package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toDocument
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toShortLink
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class ShortLinkStoreMongoDbTest : ShortLinkStoreTest {
  @Container
  private val container = MongoDBContainer(DockerImageName.parse("mongo:5.0.24"))

  companion object {
    private const val DATABASE_NAME = "shortlinks"
  }

  override val shortLinkStore: ShortLinkStore
    get() =
      ShortLinkStoreMongoDb(
        connectionString = container.getReplicaSetUrl(DATABASE_NAME),
        databaseName = DATABASE_NAME,
      )

  override suspend fun getDirect(code: ShortCode, group: ShortLinkGroup): ShortLink? {
    return collection()
      .find(Document(MongoDbFields.CODE.fieldName, code.value))
      .map { it.toShortLink() }
      .firstOrNull { it.group == group }
  }

  override suspend fun createDirect(shortLink: ShortLink): ShortLink {
    val insertResult = collection().insertOne(shortLink.toDocument())
    assertThat(insertResult.wasAcknowledged()).isTrue()
    return shortLink
  }

  private fun collection(): MongoCollection<Document> {
    val client = MongoClients.create(container.getReplicaSetUrl(DATABASE_NAME))
    val database = client.getDatabase(DATABASE_NAME)
    return database.getCollection(DATABASE_NAME)
  }
}
