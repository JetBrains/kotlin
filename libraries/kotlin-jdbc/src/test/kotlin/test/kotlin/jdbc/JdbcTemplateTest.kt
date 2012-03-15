package test.kotlin.jdbc.experiment1

import kotlin.template. *
import kotlin.jdbc. *
import kotlin.test. *
import kotlin.util. *

import test.kotlin.jdbc. *

import junit.framework.TestCase
import javax.sql.DataSource

import java.sql. *


class JdbcTemplateTest : TestCase() {
    //val dataSource = createDataSource()

    fun testTemplateInsert() {
        val id = 3
        val name = "Stepan"

        // Mimicks the following code
        // dataSource.update("insert into foo (id, name) values ($id, $name)")

        // TODO will use a tuple soon
        dataSource.update(
        StringTemplate(array("insert into foo (id, name) values (", id, ", ", name, ")"))
        )

        // Mimicks
        // datasource.query("select * from foo where id = $id") { it.map{ it["name"] } }

        val names = dataSource.query(
        StringTemplate(array("select * from foo where id = ", id))
        ) {
            it.map{ it["name"] }
        }

        println("Found names $names")
        val actual = names.first()
        assertEquals(name, actual)
    }
}
