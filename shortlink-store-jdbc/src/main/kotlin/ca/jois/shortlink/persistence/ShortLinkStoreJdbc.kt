package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.Database.DbFields
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
  }

  private val dataSource = HikariDataSource(config)

  override suspend fun listByGroupAndOwner(
    group: ShortLinkGroup,
    owner: ShortLinkUser,
    paginationKey: String?,
    limit: Int?,
  ): PaginatedResult<ShortLink> {
    val sql = """
      SELECT * 
      FROM ${Database.TableName.SHORTLINKS} 
      WHERE 1=1
        AND ${DbFields.ShortLinks.GROUP.fieldName} = ? 
        AND ${DbFields.ShortLinks.OWNER.fieldName} = ? 
      ORDER BY ${DbFields.ShortLinks.ID.fieldName} 
      LIMIT ? 
      OFFSET ?
    """.trimIndent()

    val limitOrDefault = limit ?: PAGE_SIZE
    val offset = paginationKey?.toLong() ?: 0L
    val results =
      read(sql) {
        setString(1, group.name)
        setString(2, owner.identifier)
        setInt(3, limitOrDefault)
        setLong(4, offset)
      }

    val entries = mutableListOf<ShortLink>()
    while (results.next()) {
      entries.add(
        ShortLink(
          code = ShortCode(results.getString(DbFields.ShortLinks.CODE.fieldName)),
          url = URL(results.getString(DbFields.ShortLinks.URL.fieldName)),
          createdAt = results.getLong(DbFields.ShortLinks.CREATED_AT.fieldName),
          expiresAt = results.getLongOrNull(DbFields.ShortLinks.EXPIRES_AT.fieldName),
          creator = ShortLinkUser(results.getString(DbFields.ShortLinks.CREATOR.fieldName)!!),
          owner = ShortLinkUser(results.getString(DbFields.ShortLinks.OWNER.fieldName)!!),
          group = ShortLinkGroup(results.getString(DbFields.ShortLinks.GROUP.fieldName)!!),
        ),
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
    val fieldNames =
      listOf(
        DbFields.ShortLinks.GROUP,
        DbFields.ShortLinks.CODE,
        DbFields.ShortLinks.URL,
        DbFields.ShortLinks.CREATED_AT,
        DbFields.ShortLinks.EXPIRES_AT,
        DbFields.ShortLinks.CREATOR,
        DbFields.ShortLinks.OWNER,
      ).joinToString(", ") { it.fieldName }
    try {
      val sql = """
        INSERT INTO ${Database.TableName.SHORTLINKS}
          ($fieldNames) VALUES (?, ?, ?, ?, ?, ?, ?)
      """.trimIndent()
      write(sql) {
        setString(1, shortLink.group.name)
        setString(2, shortLink.code.value)
        setString(3, shortLink.url.toString())
        setLong(4, shortLink.createdAt)
        setLongOrNull(5, shortLink.expiresAt)
        setStringOrNull(6, shortLink.creator.identifier)
        setStringOrNull(7, shortLink.owner.identifier)
      }
    } catch (e: Exception) {
      val message = e.message ?: throw e

      val isDuplicate =
        message.contains("Duplicate entry") || // mysql
          message.contains("duplicate key value violates unique constraint") // postgres
      if (isDuplicate) {
        throw ShortLinkStore.DuplicateShortCodeException(shortLink.group, shortLink.code)
      }

      throw e
    }
    return shortLink
  }

  context(Clock)
  override suspend fun get(
    code: ShortCode,
    group: ShortLinkGroup,
    excludeExpired: Boolean
  ): ShortLink? {
    val sql =
      when (excludeExpired) {
        false -> """
          SELECT * 
          FROM ${Database.TableName.SHORTLINKS} 
          WHERE 1=1
            AND ${DbFields.ShortLinks.GROUP.fieldName} = ? 
            AND ${DbFields.ShortLinks.CODE.fieldName} = ?
        """.trimIndent()

        else -> """
          SELECT * 
          FROM ${Database.TableName.SHORTLINKS} 
          WHERE 1=1
            AND ${DbFields.ShortLinks.GROUP.fieldName} = ? 
            AND ${DbFields.ShortLinks.CODE.fieldName} = ? 
            AND (${DbFields.ShortLinks.EXPIRES_AT.fieldName} is NULL or ${DbFields.ShortLinks.EXPIRES_AT.fieldName} >= ?)
        """.trimIndent()
      }

    val results =
      read(sql) {
        setString(1, group.name)
        setString(2, code.value)
        if (excludeExpired) {
          setLong(3, millis())
        }
      }

    if (results.next()) {
      return ShortLink(
        code = ShortCode(results.getString(DbFields.ShortLinks.CODE.fieldName)),
        url = URL(results.getString(DbFields.ShortLinks.URL.fieldName)),
        createdAt = results.getLong(DbFields.ShortLinks.CREATED_AT.fieldName),
        expiresAt = results.getLongOrNull(DbFields.ShortLinks.EXPIRES_AT.fieldName),
        creator = ShortLinkUser(results.getString(DbFields.ShortLinks.CREATOR.fieldName)!!),
        owner = ShortLinkUser(results.getString(DbFields.ShortLinks.OWNER.fieldName)!!),
        group = ShortLinkGroup(results.getString(DbFields.ShortLinks.GROUP.fieldName)!!),
      )
    }

    return null
  }

  override suspend fun update(
    code: ShortCode,
    url: URL,
    group: ShortLinkGroup,
    updater: ShortLinkUser
  ) {
    val sql = """
      UPDATE ${Database.TableName.SHORTLINKS} 
      SET ${DbFields.ShortLinks.URL.fieldName} = ? 
      WHERE 1=1
        AND ${DbFields.ShortLinks.GROUP.fieldName} = ? 
        AND ${DbFields.ShortLinks.CODE.fieldName} = ? 
        AND (${DbFields.ShortLinks.OWNER.fieldName} = ? OR ${DbFields.ShortLinks.OWNER.fieldName} = ?)
    """.trimIndent()

    val numUpdated =
      write(sql) {
        setString(1, url.toString())
        setString(2, group.name)
        setString(3, code.value)
        setString(4, ShortLinkUser.ANONYMOUS.identifier)
        setString(5, updater.identifier)
      }

    if (numUpdated == 0) {
      throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
    }
  }

  override suspend fun update(
    code: ShortCode,
    expiresAt: Long?,
    group: ShortLinkGroup,
    updater: ShortLinkUser
  ) {
    val sql = """
      UPDATE ${Database.TableName.SHORTLINKS} 
      SET ${DbFields.ShortLinks.EXPIRES_AT.fieldName} = ? 
      WHERE 1=1
        AND ${DbFields.ShortLinks.GROUP.fieldName} = ?
        AND ${DbFields.ShortLinks.CODE.fieldName} = ?
        AND (${DbFields.ShortLinks.OWNER.fieldName} = ? OR ${DbFields.ShortLinks.OWNER.fieldName} = ?)
    """.trimIndent()

    val numUpdated =
      write(sql) {
        setLongOrNull(1, expiresAt)
        setString(2, group.name)
        setString(3, code.value)
        setString(4, ShortLinkUser.ANONYMOUS.identifier)
        setString(5, updater.identifier)
      }

    if (numUpdated == 0) {
      throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
    }
  }

  override suspend fun delete(code: ShortCode, group: ShortLinkGroup, deleter: ShortLinkUser) {
    val sql = """
      DELETE FROM ${Database.TableName.SHORTLINKS}
      WHERE 1=1
        AND ${DbFields.ShortLinks.GROUP.fieldName} = ?
        AND ${DbFields.ShortLinks.CODE.fieldName} = ?
        AND (${DbFields.ShortLinks.OWNER.fieldName} = ? OR ${DbFields.ShortLinks.OWNER.fieldName} = ?)
    """.trimIndent()
    val numDeleted =
      write(sql) {
        setString(1, group.name)
        setString(2, code.value)
        setString(3, ShortLinkUser.ANONYMOUS.identifier)
        setString(4, deleter.identifier)
      }

    if (numDeleted == 0) {
      throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
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
