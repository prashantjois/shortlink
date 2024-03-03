package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.persistence.testhelpers.TestDatabase
import ca.jois.shortlink.persistence.testhelpers.TestDatabase.createShortLinkDirect
import ca.jois.shortlink.persistence.testhelpers.TestDatabase.getShortLinkDirect
import ca.jois.shortlink.persistence.testhelpers.TestDatabase.hikariConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.JdbcDatabaseContainer

object ShortLinkStoreMysqlTest : ShortLinkStoreJdbcTest(TestDatabase.mysql("8.3.0"))

object ShortLinkStoreMariaDbTest : ShortLinkStoreJdbcTest(TestDatabase.mariadb("11.2"))

object ShortLinkStorePostgresTest : ShortLinkStoreJdbcTest(TestDatabase.postgres("16.1"))

/**
 * Provides an abstract base for integration tests of [ShortLinkStoreTest] implementations that use
 * JDBC. This class leverages a [JdbcDatabaseContainer] to ensure each test runs against a fresh
 * instance of the database, isolating test cases and ensuring reproducible results.
 *
 * The [ShortLinkStoreJdbc] is initialized in a setup method that is called before each test case,
 * using connection parameters from the running [JdbcDatabaseContainer]. This setup and teardown
 * approach ensures that resources are properly managed and tests remain independent.
 *
 * @property container A generic JDBC database container provided at instantiation. This container
 *   is responsible for creating a temporary database environment for each test case.
 *
 * To run [ShortLinkStoreTest] against a particular type of database, simply inherit from this class
 * and pass in the container.
 *
 * Example:
 * ```
 * object ShortLinkStorePostgresTest : ShortLinkStoreJdbcTest(TestDatabase.postgres("16.1"))
 * ```
 */
abstract class ShortLinkStoreJdbcTest(private val container: JdbcDatabaseContainer<*>) :
    ShortLinkStoreTest {

    override val shortLinkStore: ShortLinkStore
        get() = shortLinkStoreJdbc

    /**
     * This property is lazily initialized to ensure the database container is started before
     * accessing the database connection parameters.
     */
    private lateinit var shortLinkStoreJdbc: ShortLinkStoreJdbc

    /**
     * Prepares the test environment before each test execution. It starts the [container] and
     * initializes [shortLinkStoreJdbc] with a HikariCP data source configured for the temporary
     * database instance.
     */
    @BeforeEach
    fun setup() {
        container.start()
        shortLinkStoreJdbc = ShortLinkStoreJdbc(container.hikariConfig())
    }

    /**
     * Cleans up the test environment after each test execution by stopping the [container],
     * effectively destroying the temporary database instance and ensuring no state is shared
     * between tests.
     */
    @AfterEach
    fun teardown() {
        container.stop()
    }

    override suspend fun getDirect(code: ShortCode, group: ShortLinkGroup): ShortLink? {
        return container.getShortLinkDirect(code, group)
    }

    override suspend fun createDirect(shortLink: ShortLink): ShortLink {
        return container.createShortLinkDirect(shortLink)
    }
}
