package test.kotlin.jdbc

import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.jdbc.*
import kotlin.test.*
import org.h2.jdbcx.JdbcConnectionPool
import org.junit.Test as test

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

class JdbcTest {
    test fun queryWithIndexColumnAccess() {
        dataSource.query("select * from foo") {
            for (row in it) {
                println("id ${row[1]} and name: ${row[2]}")
            }
        }
    }

    test fun queryWithNamedColumnAccess() {
        // query using names
        dataSource.query("select * from foo") {
            for (row in it) {
                println("name: ${row["name"]} has id ${row["id"]}")
            }
        }
    }

    test fun getValuesAsMap() {
        dataSource.query("select * from foo") {
            for (row in it) {
                println(row.getValuesAsMap())
            }
        }
    }

    test fun stringFormat() {
        dataSource.query(kotlin.template.StringTemplate(array("select * from foo where id = ", 1))) {
            for (row in it) {
                println(row.getValuesAsMap())
            }
        }
    }

    test fun mapIterator() {
        val mapper = { rs: ResultSet ->
            "id: ${rs["id"]}"
        }

        dataSource.query("select * from foo") {
            for (row in it.map(mapper)) {
                println(row)
            }
        }
    }

    test fun map() {
        dataSource.query("select * from foo") {
            val rows = it.map { "id: ${it["id"]}" }
            for (row in rows) {
                println(row)
            }
        }
    }

    test fun count() {
        dataSource.query("select count(*) from foo") {
            println("count: ${it.singleInt()}")
        }
    }

    test fun formatCursor() {
        dataSource.query("select * from foo") {
            println(it.getColumnNames().toList().makeString("\t"))

            for (row in it) {
                println(it.getValues().makeString("\t"))
            }
        }
    }
}
