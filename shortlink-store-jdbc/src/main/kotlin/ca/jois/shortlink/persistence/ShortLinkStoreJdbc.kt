package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkStore.PaginatedResult
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

        private const val PAGE_SIZE = 100
        private const val TABLE_NAME = "shortlinks"
    }

    private val dataSource = HikariDataSource(config)

    override suspend fun listByOwner(
        owner: ShortLinkUser,
        paginationKey: String?,
        limit: Int?,
    ): PaginatedResult<ShortLink> {
        val sql =
            "SELECT * FROM $TABLE_NAME WHERE ${DbFields.OWNER.name} = ? ORDER BY ${DbFields.ID.name} LIMIT ? OFFSET ?"
        val limitOrDefault = limit ?: PAGE_SIZE
        val offset = paginationKey?.toLong() ?: 0L
        val results =
            read(sql) {
                setString(1, owner.identifier)
                setInt(2, limitOrDefault)
                setLong(3, offset)
            }

        val entries = mutableListOf<ShortLink>()
        while (results.next()) {
            entries.add(
                ShortLink(
                    code = ShortCode(results.getString(DbFields.CODE.name)),
                    url = URL(results.getString(DbFields.URL.name)),
                    createdAt = results.getLong(DbFields.CREATED_AT.name),
                    expiresAt = results.getLongOrNull(DbFields.EXPIRES_AT.name),
                    creator = results.getString(DbFields.CREATOR.name)?.let { ShortLinkUser(it) },
                    owner = results.getString(DbFields.OWNER.name)?.let { ShortLinkUser(it) }
                )
            )
        }

        val nextPaginationKey =
            when (entries.size < limitOrDefault) {
                true -> null
                false -> (offset + limitOrDefault).toString()
            }

        return PaginatedResult(entries, nextPaginationKey)
    }

    override suspend fun create(shortLink: ShortLink): ShortLink {
        try {
            write(
                "INSERT INTO $TABLE_NAME (${DbFields.CODE.name}, ${DbFields.URL.name}, ${DbFields.CREATED_AT.name}, ${DbFields.EXPIRES_AT.name}, ${DbFields.CREATOR.name}, ${DbFields.OWNER.name}) VALUES (?, ?, ?, ?, ?, ?)"
            ) {
                setString(1, shortLink.code.value)
                setString(2, shortLink.url.toString())
                setLong(3, shortLink.createdAt)
                setLongOrNull(4, shortLink.expiresAt)
                setStringOrNull(5, shortLink.creator?.identifier)
                setStringOrNull(6, shortLink.owner?.identifier)
            }
        } catch (e: Exception) {
            val message = e.message ?: throw e

            val isDuplicate =
                message.contains("Duplicate entry") || // mysql
                    message.contains("duplicate key value violates unique constraint") // postgres
            if (isDuplicate) {
                throw ShortLinkStore.DuplicateShortCodeException(shortLink.code)
            }

            throw e
        }
        return shortLink
    }

    context(Clock)
    override suspend fun get(code: ShortCode, excludeExpired: Boolean): ShortLink? {
        val sql =
            when (excludeExpired) {
                false -> "SELECT * FROM $TABLE_NAME WHERE ${DbFields.CODE.name} = ?"
                else ->
                    "SELECT * FROM $TABLE_NAME WHERE ${DbFields.CODE.name} = ? and (${DbFields.EXPIRES_AT.name} is NULL or ${DbFields.EXPIRES_AT.name} >= ?)"
            }

        val results =
            read(sql) {
                setString(1, code.value)
                if (excludeExpired) {
                    setLong(2, millis())
                }
            }

        if (results.next()) {
            return ShortLink(
                code = ShortCode(results.getString(DbFields.CODE.name)),
                url = URL(results.getString(DbFields.URL.name)),
                createdAt = results.getLong(DbFields.CREATED_AT.name),
                expiresAt = results.getLongOrNull(DbFields.EXPIRES_AT.name),
                creator = results.getString(DbFields.CREATOR.name)?.let { ShortLinkUser(it) },
                owner = results.getString(DbFields.OWNER.name)?.let { ShortLinkUser(it) }
            )
        }

        return null
    }

    override suspend fun update(updater: ShortLinkUser?, code: ShortCode, url: URL) {
        val sql =
            when (updater) {
                null ->
                    "UPDATE $TABLE_NAME SET ${DbFields.URL.name} = ? WHERE ${DbFields.CODE.name} = ? AND ${DbFields.OWNER.name} is NULL"
                else ->
                    "UPDATE $TABLE_NAME SET ${DbFields.URL.name} = ? WHERE ${DbFields.CODE.name} = ? AND (${DbFields.OWNER.name} is NULL OR ${DbFields.OWNER.name} = ?)"
            }

        val numUpdated =
            write(sql) {
                setString(1, url.toString())
                setString(2, code.value)
                updater?.let { setString(3, it.identifier) }
            }

        if (numUpdated == 0) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
    }

    override suspend fun update(updater: ShortLinkUser?, code: ShortCode, expiresAt: Long?) {
        val sql =
            when (updater) {
                null ->
                    "UPDATE $TABLE_NAME SET ${DbFields.EXPIRES_AT.name} = ? WHERE ${DbFields.CODE.name} = ? AND ${DbFields.OWNER.name} is NULL"
                else ->
                    "UPDATE $TABLE_NAME SET ${DbFields.EXPIRES_AT.name} = ? WHERE ${DbFields.CODE.name} = ? AND (${DbFields.OWNER.name} is NULL OR ${DbFields.OWNER.name} = ?)"
            }

        val numUpdated =
            write(sql) {
                setLongOrNull(1, expiresAt)
                setString(2, code.value)
                updater?.let { setString(3, it.identifier) }
            }

        if (numUpdated == 0) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
        }
    }

    override suspend fun delete(deleter: ShortLinkUser?, code: ShortCode) {
        val sql =
            when (deleter) {
                null ->
                    "DELETE FROM $TABLE_NAME WHERE ${DbFields.CODE.name} = ? AND ${DbFields.OWNER.name} is NULL"
                else ->
                    "DELETE FROM $TABLE_NAME WHERE ${DbFields.CODE.name} = ? AND (${DbFields.OWNER.name} is NULL OR ${DbFields.OWNER.name} = ?)"
            }
        val numDeleted =
            write(sql) {
                setString(1, code.value)
                deleter?.let { setString(2, it.identifier) }
            }

        if (numDeleted == 0) {
            throw ShortLinkStore.NotFoundOrNotPermittedException(code)
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

private fun ResultSet.getLongOrNull(name: String): Long /**/? {
    return getLong(name).let {
        when (it) {
            0L -> null
            else -> getLong(name)
        }
    }
}

private fun PreparedStatement.setLongOrNull(i: Int, value: Long?) {
    when (value) {
        null -> setNull(i, java.sql.Types.BIGINT)
        else -> setLong(i, value)
    }
}

private fun PreparedStatement.setStringOrNull(i: Int, value: String?) {
    when (value) {
        null -> setNull(i, java.sql.Types.VARCHAR)
        else -> setString(i, value)
    }
}
