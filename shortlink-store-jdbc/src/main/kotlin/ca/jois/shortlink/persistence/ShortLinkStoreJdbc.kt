package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.Database.TableName
import ca.jois.shortlink.persistence.ShortLinkStore.PaginatedResult
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.URL
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.Clock
import ca.jois.shortlink.persistence.Database.DbFields.ShortLinkGroups as ShortLinkGroupsFields
import ca.jois.shortlink.persistence.Database.DbFields.ShortLinks as ShortLinksField

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
      FROM ${TableName.SHORTLINKS} s
      INNER JOIN ${TableName.SHORTLINK_GROUPS} g
        ON s.${ShortLinksField.GROUP_ID.fieldName} = g.${ShortLinkGroupsFields.ID.fieldName}
      WHERE 1=1
        AND ${ShortLinkGroupsFields.NAME.fieldName} = ?
        AND ${ShortLinksField.OWNER.fieldName} = ? 
      ORDER BY s.${ShortLinksField.ID.fieldName} 
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
          code = ShortCode(results.getString(ShortLinksField.CODE.fieldName)),
          url = URL(results.getString(ShortLinksField.URL.fieldName)),
          createdAt = results.getLong(ShortLinksField.CREATED_AT.fieldName),
          expiresAt = results.getLongOrNull(ShortLinksField.EXPIRES_AT.fieldName),
          creator = ShortLinkUser(results.getString(ShortLinksField.CREATOR.fieldName)!!),
          owner = ShortLinkUser(results.getString(ShortLinksField.OWNER.fieldName)!!),
          group = ShortLinkGroup(results.getString(ShortLinkGroupsFields.NAME.fieldName)!!),
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
    try {
      upsertGroup(shortLink.group)
      val sql = """
        INSERT INTO ${TableName.SHORTLINKS} (
            ${ShortLinksField.GROUP_ID.fieldName},
            ${ShortLinksField.CODE.fieldName},
            ${ShortLinksField.URL.fieldName},
            ${ShortLinksField.CREATED_AT.fieldName},
            ${ShortLinksField.EXPIRES_AT.fieldName},
            ${ShortLinksField.CREATOR.fieldName},
            ${ShortLinksField.OWNER.fieldName}
        )
        VALUES (
          (SELECT ID FROM ${TableName.SHORTLINK_GROUPS} WHERE ${ShortLinkGroupsFields.NAME.fieldName} = ?),
          ?,
          ?,
          ?,
          ?,
          ?,
          ?
        )
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

  private fun upsertGroup(group: ShortLinkGroup) {
    val selectSql = """
      SELECT ${ShortLinkGroupsFields.ID.fieldName} 
      FROM ${TableName.SHORTLINK_GROUPS} 
      WHERE ${ShortLinkGroupsFields.NAME.fieldName} = ?
    """.trimIndent()

    val selectResults = read(selectSql) {
      setString(1, group.name)
    }

    if (selectResults.next()) return

    val insertSql = """
      INSERT INTO ${TableName.SHORTLINK_GROUPS}
      (${ShortLinkGroupsFields.NAME.fieldName}) 
      VALUES
      (?)
    """.trimIndent()

    write(insertSql) {
      setString(1, group.name)
    }
  }

  context(Clock)
  override suspend fun get(
    code: ShortCode,
    group: ShortLinkGroup,
    excludeExpired: Boolean
  ): ShortLink? {
    var sql = """
      SELECT * 
      FROM ${TableName.SHORTLINKS} s
      INNER JOIN ${TableName.SHORTLINK_GROUPS} g
        ON s.${ShortLinksField.GROUP_ID.fieldName} = g.${ShortLinkGroupsFields.ID.fieldName}
      WHERE 1=1
        AND ${ShortLinkGroupsFields.NAME.fieldName} = ?
        AND ${ShortLinksField.CODE.fieldName} = ?
    """.trimIndent()

    sql = when (excludeExpired) {
      true -> "$sql AND (${ShortLinksField.EXPIRES_AT.fieldName} is NULL or ${ShortLinksField.EXPIRES_AT.fieldName} >= ?)"
      false -> sql
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
        code = ShortCode(results.getString(ShortLinksField.CODE.fieldName)),
        url = URL(results.getString(ShortLinksField.URL.fieldName)),
        createdAt = results.getLong(ShortLinksField.CREATED_AT.fieldName),
        expiresAt = results.getLongOrNull(ShortLinksField.EXPIRES_AT.fieldName),
        creator = ShortLinkUser(results.getString(ShortLinksField.CREATOR.fieldName)!!),
        owner = ShortLinkUser(results.getString(ShortLinksField.OWNER.fieldName)!!),
        group = ShortLinkGroup(results.getString(ShortLinkGroupsFields.NAME.fieldName)!!),
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
      UPDATE ${TableName.SHORTLINKS} 
      SET ${ShortLinksField.URL.fieldName} = ? 
      WHERE 1=1
        AND ${ShortLinksField.GROUP_ID.fieldName} = (
          SELECT ${ShortLinkGroupsFields.ID.fieldName} 
            FROM ${TableName.SHORTLINK_GROUPS} 
            WHERE ${ShortLinkGroupsFields.NAME.fieldName} = ?
        ) 
        AND ${ShortLinksField.CODE.fieldName} = ? 
        AND (${ShortLinksField.OWNER.fieldName} = ? OR ${ShortLinksField.OWNER.fieldName} = ?)
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
      UPDATE ${TableName.SHORTLINKS} 
      SET ${ShortLinksField.EXPIRES_AT.fieldName} = ? 
      WHERE 1=1
        AND ${ShortLinksField.GROUP_ID.fieldName} = (
          SELECT ${ShortLinkGroupsFields.ID.fieldName} 
            FROM ${TableName.SHORTLINK_GROUPS} 
            WHERE ${ShortLinkGroupsFields.NAME.fieldName} = ?
        ) 
        AND ${ShortLinksField.CODE.fieldName} = ?
        AND (${ShortLinksField.OWNER.fieldName} = ? OR ${ShortLinksField.OWNER.fieldName} = ?)
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
      DELETE FROM ${TableName.SHORTLINKS}
      WHERE 1=1
        AND ${ShortLinksField.GROUP_ID.fieldName} = (
          SELECT ${ShortLinkGroupsFields.ID.fieldName} 
            FROM ${TableName.SHORTLINK_GROUPS} 
            WHERE ${ShortLinkGroupsFields.NAME.fieldName} = ?
        ) 
        AND ${ShortLinksField.CODE.fieldName} = ?
        AND (${ShortLinksField.OWNER.fieldName} = ? OR ${ShortLinksField.OWNER.fieldName} = ?)
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
