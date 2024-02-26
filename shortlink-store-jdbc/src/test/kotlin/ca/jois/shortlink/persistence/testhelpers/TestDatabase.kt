package ca.jois.shortlink.persistence.testhelpers

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.persistence.ShortLinkStore
import ca.jois.shortlink.persistence.ShortLinkStoreJdbc
import ca.jois.shortlink.testhelpers.factory.ShortLinkFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.URL
import java.sql.PreparedStatement
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Helpers with factory methods, extension methods and other helpers to aid in testing against test
 * databases.
 */
object TestDatabase {
    private const val DB_NAME = "shortlinks_test"

    /** Build a MySQL docker container with the specified version */
    fun mysql(version: String, dbName: String = DB_NAME): MySQLContainer<*> {
        return MySQLContainer(DockerImageName.parse("mysql:$version"))
            .withDatabaseName(dbName)
            .withInitScript("mysql-init.sql")
    }

    /** Build a MariaDB docker container with the specified version */
    fun mariadb(version: String, dbName: String = DB_NAME): MariaDBContainer<*> {
        return MariaDBContainer(DockerImageName.parse("mariadb:$version"))
            .withDatabaseName(dbName)
            .withInitScript("mysql-init.sql")
    }

    /** Build a PostgreSQL docker container with the specified version */
    fun postgres(version: String, dbName: String = DB_NAME): PostgreSQLContainer<*> {
        return PostgreSQLContainer(DockerImageName.parse("postgres:$version"))
            .withDatabaseName(dbName)
            .withInitScript("postgresql-init.sql")
    }

    /** Test helper to wrap container starting and stopping. */
    suspend fun initShortLinkStore(
        container: JdbcDatabaseContainer<*>,
        test: suspend (ShortLinkStore) -> Unit
    ) {
        container.start()
        try {
            test(ShortLinkStoreJdbc(container.hikariConfig()))
        } finally {
            container.stop()
        }
    }

    /**
     * Test helper to get a shortlink directly from the database. This allows you to test without
     * depending on application functionality to read the database.
     */
    fun JdbcDatabaseContainer<*>.getShortLinkDirect(code: ShortCode): ShortLink? {
        val selectSql = "SELECT * FROM shortlinks WHERE code = ?"

        val connection = HikariDataSource(hikariConfig()).connection
        val stmt: PreparedStatement =
            connection.prepareStatement(selectSql).apply { setString(1, code.value) }

        stmt.executeQuery().let { rs ->
            if (!rs.next()) {
                return null
            }
            val expiresAt =
                when (val expiresAtRaw = rs.getLong("expires_at")) {
                    0L -> null
                    else -> expiresAtRaw
                }

            return ShortLink(
                code = ShortCode(rs.getString("code")),
                url = URL(rs.getString("url")),
                createdAt = rs.getLong("created_at"),
                expiresAt = expiresAt,
            )
        }
    }

    /**
     * Test helper to get a shortlink directly from the database. This allows you to test without
     * depending on application functionality to write to the database.
     */
    fun JdbcDatabaseContainer<*>.createShortLinkDirect(
        shortLink: ShortLink = ShortLinkFactory.build()
    ): ShortLink {
        val insertSql =
            "INSERT INTO shortlinks (code, url, created_at, expires_at, owner) VALUES (?, ?, ?, ?, ?)"

        val connection = HikariDataSource(hikariConfig()).connection
        connection
            .prepareStatement(insertSql)
            .apply {
                setString(1, shortLink.code.value)
                setString(2, shortLink.url.toString())
                setLong(3, shortLink.createdAt)
                shortLink.expiresAt?.let { setLong(4, it) } ?: setNull(4, java.sql.Types.BIGINT)
                shortLink.owner?.let { setString(5, it.identifier) }
                    ?: setNull(5, java.sql.Types.VARCHAR)
            }
            .executeUpdate()

        return shortLink
    }

    fun JdbcDatabaseContainer<*>.hikariConfig(): HikariConfig {
        return HikariConfig().also {
            it.jdbcUrl = jdbcUrl
            it.username = username
            it.password = password
        }
    }
}
