package ca.jois.shortlink.persistence.testhelpers

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.Database.DbFields
import ca.jois.shortlink.testhelpers.factory.ShortLinkFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.net.URL
import java.sql.PreparedStatement

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

  /**
   * Test helper to get a shortlink directly from the database. This allows you to test without
   * depending on application functionality to read the database.
   */
  fun JdbcDatabaseContainer<*>.getShortLinkDirect(
    code: ShortCode,
    group: ShortLinkGroup
  ): ShortLink? {
    val selectSql =
      "SELECT * FROM shortlinks WHERE ${DbFields.ShortLinks.CODE.fieldName} = ? AND ${DbFields.ShortLinks.GROUP.fieldName} = ?"

    val connection = HikariDataSource(hikariConfig()).connection
    val stmt: PreparedStatement =
      connection.prepareStatement(selectSql).apply {
        setString(1, code.value)
        setString(2, group.name)
      }

    stmt.executeQuery().let { rs ->
      if (!rs.next()) {
        return null
      }
      val expiresAt =
        when (val expiresAtRaw = rs.getLong(DbFields.ShortLinks.EXPIRES_AT.fieldName)) {
          0L -> null
          else -> expiresAtRaw
        }

      return ShortLink(
        code = ShortCode(rs.getString(DbFields.ShortLinks.CODE.fieldName)),
        url = URL(rs.getString(DbFields.ShortLinks.URL.fieldName)),
        createdAt = rs.getLong(DbFields.ShortLinks.CREATED_AT.fieldName),
        expiresAt = expiresAt,
        creator = ShortLinkUser(rs.getString(DbFields.ShortLinks.CREATOR.fieldName)),
        owner = ShortLinkUser(rs.getString(DbFields.ShortLinks.OWNER.fieldName)),
        group = ShortLinkGroup(rs.getString(DbFields.ShortLinks.GROUP.fieldName)),
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
    val fieldNames =
      listOf(
        DbFields.ShortLinks.GROUP,
        DbFields.ShortLinks.CODE,
        DbFields.ShortLinks.URL,
        DbFields.ShortLinks.CREATED_AT,
        DbFields.ShortLinks.EXPIRES_AT,
        DbFields.ShortLinks.CREATOR,
        DbFields.ShortLinks.OWNER,
      )
        .joinToString(", ") { it.fieldName }

    val insertSql = "INSERT INTO shortlinks ($fieldNames) VALUES (?, ?, ?, ?, ?, ?, ?)"

    val connection = HikariDataSource(hikariConfig()).connection
    connection
      .prepareStatement(insertSql)
      .apply {
        setString(1, shortLink.group.name)
        setString(2, shortLink.code.value)
        setString(3, shortLink.url.toString())
        setLong(4, shortLink.createdAt)
        shortLink.expiresAt?.let { setLong(5, it) } ?: setNull(5, java.sql.Types.BIGINT)
        setString(6, shortLink.creator.identifier)
        setString(7, shortLink.owner.identifier)
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
