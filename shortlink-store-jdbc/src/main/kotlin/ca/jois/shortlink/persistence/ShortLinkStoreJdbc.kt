package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.URL
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.Clock

/**
 * Implementation of [ShortLinkStore] that uses raw SQL queries through JDBC connections.
 *
 * @param config The config specifying the connection, which should ideally be loaded from a config
 *   file.
 *
 * Example usage:
 * ```
 * val shortLinkStore = ShortLinkStoreJdbc.configure {
 *   config.jdbcUrl = "jdbc:postgresql://localhost:5432/dbname"
 *   config.username = "username"
 *   config.password = "password
 * }
 * ```
 */
class ShortLinkStoreJdbc(config: HikariConfig) : ShortLinkStore {
    companion object {
        fun configure(buildConfig: HikariConfig.() -> Unit): ShortLinkStoreJdbc {
            val config = HikariConfig()
            buildConfig(config)
            return ShortLinkStoreJdbc(config)
        }
    }

    private val dataSource = HikariDataSource(config)

    override suspend fun create(shortLink: ShortLink): ShortLink {
        try {
            write(
                "INSERT INTO shortlinks (code, url, created_at, expires_at) VALUES (?, ?, ?, ?)"
            ) {
                setString(1, shortLink.code.code)
                setString(2, shortLink.url.toString())
                setLong(3, shortLink.createdAt)
                setLongOrNull(4, shortLink.expiresAt)
            }
        } catch (e: Exception) {
            val message = e.message ?: throw e

            val isDuplicate =
                message.contains("Duplicate entry") || // mysql
                    message.contains("duplicate key value violates unique constraint") // postgres
            if (isDuplicate) {
                throw ShortLinkStore.DuplicateShortCodeException(shortLink.code.code)
            }

            throw e
        }
        return shortLink
    }

    context(Clock)
    override suspend fun get(code: ShortCode, excludeExpired: Boolean): ShortLink? {
        val sql =
            when (excludeExpired) {
                false -> "SELECT * FROM shortlinks WHERE code = ?"
                else ->
                    "SELECT * FROM shortlinks WHERE code = ? and (expires_at is NULL or expires_at >= ?)"
            }

        val results =
            read(sql) {
                setString(1, code.code)
                if (excludeExpired) {
                    setLong(2, millis())
                }
            }

        if (results.next()) {
            // getLong returns 0 if the value is NULL
            val expiresAt =
                when (val expiresAtRaw = results.getLong("expires_at")) {
                    0L -> null
                    else -> expiresAtRaw
                }

            return ShortLink(
                code = ShortCode(results.getString("code")),
                url = URL(results.getString("url")),
                createdAt = results.getLong("created_at"),
                expiresAt = expiresAt,
            )
        }

        return null
    }

    override suspend fun update(code: ShortCode, url: URL) {
        val numUpdated =
            write("UPDATE shortlinks SET url = ? WHERE code = ?") {
                setString(1, url.toString())
                setString(2, code.code)
            }

        if (numUpdated == 0) {
            throw ShortLinkStore.NotFoundException(code)
        }
    }

    override suspend fun update(code: ShortCode, expiresAt: Long?) {
        val numUpdated =
            write("UPDATE shortlinks SET expires_at = ? WHERE code = ?") {
                setLongOrNull(1, expiresAt)
                setString(2, code.code)
            }

        if (numUpdated == 0) {
            throw ShortLinkStore.NotFoundException(code)
        }
    }

    override suspend fun delete(code: ShortCode) {
        val numDeleted = write("DELETE FROM shortlinks WHERE code = ?") { setString(1, code.code) }

        if (numDeleted == 0) {
            throw ShortLinkStore.NotFoundException(code)
        }
    }

    private fun prepareStatement(
        sql: String,
        handle: PreparedStatement.() -> Unit
    ): PreparedStatement {
        val conn = dataSource.connection
        val preparedStatement = conn.prepareStatement(sql)
        handle(preparedStatement)
        return preparedStatement
    }

    private fun write(sql: String, handle: PreparedStatement.() -> Unit): Int {
        val preparedStatement = prepareStatement(sql) { handle(this) }
        return preparedStatement.executeUpdate()
    }

    private fun read(sql: String, handle: PreparedStatement.() -> Unit): ResultSet {
        val preparedStatement = prepareStatement(sql) { handle(this) }
        return preparedStatement.executeQuery()
    }
}

private fun PreparedStatement.setLongOrNull(i: Int, value: Long?) {
    when (value) {
        null -> setNull(i, java.sql.Types.BIGINT)
        else -> setLong(i, value)
    }
}
