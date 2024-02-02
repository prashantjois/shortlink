package shortlinkapp

import ca.jois.shortlink.persistence.ShortLinkStoreJdbc
import shortlinkapp.api.service.WebServer

fun main() {
    // Uncomment to use in-memory storage
    // val shortLinkStore = InMemoryShortLinkStore()

    // Uncomment to use database server
    val shortLinkStore =
        ShortLinkStoreJdbc.configure {
            jdbcUrl = "jdbc:mysql://127.0.0.1:3306/shortlinks"
            username = "root"
        }

    WebServer(port = 8080, shortLinkStore = shortLinkStore).run()
}
