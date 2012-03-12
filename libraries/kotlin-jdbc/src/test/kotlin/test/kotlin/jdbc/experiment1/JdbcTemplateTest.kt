package test.kotlin.jdbc.experiment1

import kotlin.template.experiment1.*
import kotlin.jdbc.*
import kotlin.test.*
import kotlin.util.arrayList

import junit.framework.TestCase
import javax.sql.DataSource

import test.kotlin.jdbc.createDataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

fun DataSource.update(fn: (PreparedStatementBuilder) -> Unit): Int {
    return use{ it.update(fn) }
}

fun <T> DataSource.query(fn: (PreparedStatementBuilder) -> Unit, resultBlock: (ResultSet) -> T): T {
    return use{ it.query(fn, resultBlock) }
}

fun Connection.update(fn: (PreparedStatementBuilder) -> Unit): Int {
    val preparedStatement = prepare(fn)
    return preparedStatement.update()
}


fun <T> Connection.query(fn: (PreparedStatementBuilder) -> Unit, resultBlock: (ResultSet) -> T): T {
    val preparedStatement = prepare(fn)
    return preparedStatement.query(resultBlock)
}

fun Connection.prepare(fn: (PreparedStatementBuilder) -> Unit): PreparedStatement {
    val builder = PreparedStatementBuilder(this)
    fn(builder)
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

    fun expression(value: String): Unit {
        sql.append("?")
        tasks.add(object: Binding {
            override fun bind(statement : PreparedStatement) {
                statement.setString(nextParameterIndex(), value)
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
        val id = 1

/*
        dataSource.update {
            it.text("select * from foo where id = ")
            it.expression(id)
        }
*/

    }
}
