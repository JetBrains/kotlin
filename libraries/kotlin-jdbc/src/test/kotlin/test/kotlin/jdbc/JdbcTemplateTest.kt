package test.kotlin.jdbc.experiment1

import kotlin.template.*
import kotlin.jdbc.*
import kotlin.test.*
import kotlin.util.*

import test.kotlin.jdbc.*

import junit.framework.TestCase
import javax.sql.DataSource

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

fun DataSource.update(template: StringTemplate): Int {
    return use{ it.update(template) }
}

fun <T> DataSource.query(template: StringTemplate, resultBlock: (ResultSet) -> T): T {
    return use{ it.query(template, resultBlock) }
}

fun Connection.update(template: StringTemplate): Int {
    val preparedStatement = prepare(template)
    return preparedStatement.update()
}


fun <T> Connection.query(template: StringTemplate, resultBlock: (ResultSet) -> T): T {
    val preparedStatement = prepare(template)
    return preparedStatement.query(resultBlock)
}

fun Connection.prepare(template: StringTemplate): PreparedStatement {
    val builder = PreparedStatementBuilder(this)
    var constantText = true
    template.forEach{
        if (constantText) {
            if (it == null) {
                throw IllegalStateException("No constant checks should be null");
            } else {
                val text = it.toString()
                if (text != null) {
                    builder.text(text)
                }
            }
        } else {
            builder.expression(it)
        }
        constantText = !constantText
    }
    return builder.statement()
}

trait Binding {
    fun bind(statement: PreparedStatement): Unit
}

class PreparedStatementBuilder(val connection: Connection) {
    val sql = StringBuilder()
    val tasks = arrayList<Binding>()
    private var parameterIndex = 0

    fun text(value: String): Unit {
        // we assume all text is escaped already
        sql.append(value)
    }

    fun expression(value: Any?): Unit {
        if (value is String) {
            expression(value)
        } else if (value is Int) {
            expression(value)
        } else {

        }
    }

    fun expression(value: String): Unit {
        sql.append("?")
        tasks.add(object: Binding {
            override fun bind(statement : PreparedStatement) {
                statement.setString(nextParameterIndex(), value)
            }
        })
    }

    fun expression(value: Int): Unit {
        sql.append("?")
        tasks.add(object: Binding {
            override fun bind(statement : PreparedStatement) {
                statement.setInt(nextParameterIndex(), value)
            }
        })
    }

    // TODO bind other kinds!

    fun statement(): PreparedStatement {
        val answer = connection.prepareStatement(sql.toString())
        if (answer == null) {
            throw IllegalStateException("No PreparedStatement returned from $connection")
        } else {
            for (task in tasks) {
                task.bind(answer)
            }
            return answer
        }
    }

    protected fun nextParameterIndex(): Int = ++parameterIndex
}


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
