package test.kotlin.jdbc.experiment1

import kotlin.jdbc.*
import kotlin.template.*
import kotlin.test.*
import org.junit.Test as test
import test.kotlin.jdbc.*

class JdbcTemplateTest {
    //val dataSource = createDataSource()

    @test fun templateInsert() {
        val id = 3
        val name = "Stepan"

        // Mimicks the following code
        // dataSource.update("insert into foo (id, name) values ($id, $name)")

        // TODO will use a tuple soon
        dataSource.update(
        StringTemplate(arrayOf("insert into foo (id, name) values (", id, ", ", name, ")"))
        )

        // Mimicks
        // datasource.query("select * from foo where id = $id") { it.map{ it["name"] } }

        val names = dataSource.query(
        StringTemplate(arrayOf("select * from foo where id = ", id))
        ) {
            it.map{ it["name"] }.toList()
        }

        println("Found names $names")
        val actual = names.first()
        assertEquals(name, actual)
    }
}
