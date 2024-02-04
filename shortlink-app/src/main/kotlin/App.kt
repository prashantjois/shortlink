package shortlinkapp

import ca.jois.shortlink.persistence.ShortLinkStoreInMemory
import ca.jois.shortlink.persistence.ShortLinkStoreJdbc
import ca.jois.shortlink.persistence.ShortLinkStoreMongoDb
import shortlinkapp.api.service.WebServer

fun main() {
    // Uncomment to use in-memory storage
    //  val shortLinkStore = inMem()

    // Uncomment to use MySQL database server
    //  val shortLinkStore = jdbc()

    // Uncomment to use MongoDB database server
    val shortLinkStore = mongo()

    WebServer(port = 8080, shortLinkStore = shortLinkStore).run()
}

private fun inMem() = ShortLinkStoreInMemory()

private fun jdbc() =
    ShortLinkStoreJdbc.configure {
        jdbcUrl = "jdbc:mysql://127.0.0.1:3306/shortlinks"
        username = "root"
    }

private fun mongo() =
    ShortLinkStoreMongoDb(
        connectionString = "mongodb://127.0.0.1:27017/shortlinks",
        databaseName = "shortlinks",
        collectionName = "shortlinks",
    )
