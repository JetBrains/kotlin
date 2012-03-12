package test.kotlin.jdbc

import kotlin.jdbc.*
import kotlin.test.*

import junit.framework.TestCase
import org.h2.jdbcx.JdbcDataSource
import javax.sql.DataSource
import org.h2.jdbcx.JdbcConnectionPool

fun createDataSource(): DataSource {
    val pool = JdbcConnectionPool.create("jdbc:h2:mem:KotlinJdbcTest;DB_CLOSE_DELAY=-1", "user", "password")
    if (pool == null) {
        throw IllegalStateException("No DataSource created")
    } else {
        return pool
    }
}

class JdbcTest : TestCase() {
    val dataSource = createDataSource()

    fun testDatabase() {
        dataSource.update("create table foo (id int primary key, name varchar(100))")
        assertEquals(1, dataSource.update("insert into foo (id, name) values (1, 'James')"))
        assertEquals(1, dataSource.update("insert into foo (id, name) values (2, 'Andrey')"))

        // lets query using integer lookups
        dataSource.query("select * from foo") {
            for (row in it) {
                println("id ${row[1]} and name: ${row[2]}")
            }
        }

        // query using names
        dataSource.query("select * from foo") {
            for (row in it) {
                println("name: ${row["name"]} has id ${row["id"]}")
            }
        }
    }
}