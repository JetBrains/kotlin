package test.kotlin.jdbc

import kotlin.jdbc. *
import kotlin.test. *

import junit.framework.TestCase
import org.h2.jdbcx.JdbcDataSource
import javax.sql.DataSource
import org.h2.jdbcx.JdbcConnectionPool
import kotlin.nullable.forEach
import java.sql.ResultSet

public val dataSource : DataSource = createDataSource()

fun createDataSource() : DataSource {
    val dataSource = JdbcConnectionPool.create("jdbc:h2:mem:KotlinJdbcTest;DB_CLOSE_DELAY=-1", "user", "password")
    if (dataSource == null) {
        throw IllegalStateException("No DataSource created")
    } else {
        dataSource.update("create table foo (id int primary key, name varchar(100))")
        assertEquals(1, dataSource.update("insert into foo (id, name) values (1, 'James')"))
        assertEquals(1, dataSource.update("insert into foo (id, name) values (2, 'Andrey')"))

        return dataSource
    }
}

class JdbcTest : TestCase() {
    fun testQueryWithIndexColumnAccess() {
        dataSource.query("select * from foo") {
            for (row in it) {
                println("id ${row[1]} and name: ${row[2]}")
            }
        }
    }

    fun testQueryWithNamedColumnAccess() {
        // query using names
        dataSource.query("select * from foo") {
            for (row in it) {
                println("name: ${row["name"]} has id ${row["id"]}")
            }
        }
    }

    fun testGetValuesAsMap() {
        dataSource.query("select * from foo") {
            for (row in it) {
                println(row.getValuesAsMap())
            }
        }
    }

    fun testStringFormat() {
        dataSource.query(kotlin.template.StringTemplate(array("select * from foo where id = ", 1))) {
            for (row in it) {
                println(row.getValuesAsMap())
            }
        }
    }

    fun testMapIterator() {
        val mapper = { (rs : ResultSet) ->
            "id: ${rs["id"]}"
        }

        dataSource.query("select * from foo") {
            for (row in it.getMapped(mapper)) {
                println(row)
            }
        }
    }

    fun testCount() {
        dataSource.query("select count(*) from foo") {
            println("count: ${it.singleInt()}")
        }
    }

    fun testFormatCursor() {
        dataSource.query("select * from foo") {
            println(it.getColumnNames().toList().makeString("\t"))

            for (row in it) {
                println(it.getValues().makeString("\t"))
            }
        }
    }
}
