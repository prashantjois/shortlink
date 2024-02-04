package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toDocument
import ca.jois.shortlink.persistence.ShortLinkMongoDbExtensions.toShortLink
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import org.bson.Document
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class ShortLinkStoreMongoDbTest : ShortLinkStoreTest {
    @Container private val container = MongoDBContainer(DockerImageName.parse("mongo:5.0.24"))

    private val name = "shortlinks"

    override val shortLinkStore: ShortLinkStore
        get() =
            ShortLinkStoreMongoDb(
                connectionString = container.getReplicaSetUrl(name),
                databaseName = name,
                collectionName = name
            )

    override suspend fun getDirect(code: ShortCode): ShortLink? {
        return collection()
            .find(Document(MongoDbFields.CODE.name, code.code))
            .firstOrNull()
            ?.toShortLink()
    }

    override suspend fun createDirect(shortLink: ShortLink): ShortLink {
        collection().insertOne(shortLink.toDocument())
        return shortLink
    }

    private fun collection(): MongoCollection<Document> {
        val client = MongoClients.create(container.getReplicaSetUrl(name))
        val database = client.getDatabase(name)
        return database.getCollection(name)
    }
}
