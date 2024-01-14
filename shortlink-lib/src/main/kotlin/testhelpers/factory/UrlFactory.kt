package testhelpers.factory

import java.net.URL
import kotlin.random.Random

object UrlFactory {
    /**
     * Generates a random URL.
     *
     * This function creates a URL by randomly selecting a protocol (HTTP or HTTPS), generating a
     * domain name of random length (between 5 and 10 characters) using alphanumeric characters, and
     * appending a random domain extension (.com, .net, .org, .io).
     */
    fun random(
        protocols: List<String> = listOf("http", "https"),
        domains: List<String> = listOf(".com", ".net", ".org", ".io"),
        characters: String = "abcdefghijklmnopqrstuvwxyz0123456789",
        length: Int = Random.nextInt(5, 11)
    ): URL {
        // Select a random protocol
        val protocol = protocols.random()

        // Generate a random domain name of length 5-10 characters
        val domainName = (1..length).map { characters.random() }.joinToString("")

        // Select a random domain
        val domain = domains.random()

        return URL("$protocol://$domainName$domain")
    }
}
