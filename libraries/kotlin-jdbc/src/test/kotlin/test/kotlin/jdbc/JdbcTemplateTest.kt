package test.kotlin.jdbc.experiment1

import kotlin.template. *
import kotlin.jdbc. *
import kotlin.test. *
import kotlin.util. *

import test.kotlin.jdbc. *

import junit.framework.TestCase
import javax.sql.DataSource

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp

fun DataSource.update(template : StringTemplate) : Int {
    return use{ it.update(template) }
}

fun <T> DataSource.query(template : StringTemplate, resultBlock : (ResultSet) -> T) : T {
    return use{ it.query(template, resultBlock) }
}

fun Connection.update(template : StringTemplate) : Int {
    val preparedStatement = prepare(template)
    return preparedStatement.update()
}


fun <T> Connection.query(template : StringTemplate, resultBlock : (ResultSet) -> T) : T {
    val preparedStatement = prepare(template)
    return preparedStatement.query(resultBlock)
}

fun Connection.prepare(template : StringTemplate) : PreparedStatement {
    val builder = PreparedStatementBuilder(template, this)
    builder.bind()
    return builder.statement
}

trait Binding {
    fun bind(statement : PreparedStatement) : Unit
}

class PreparedStatementBuilder(val template : StringTemplate, val connection : Connection) {
    private var parameterIndex = 0

    public val sql : String = createSql()

    public val statement: PreparedStatement = lookupOrCreateStatement()

    /**
     * Binds the values in the [[StringTemplate]] to the [[PreparedStatement]]
     */
    fun bind() {
        var constantText = true
        template.forEach{
            if (!constantText) {
                expression(it)
            }
            constantText = !constantText
        }
    }

    fun expression(value : Any?) : Unit {
        // TODO causes compiler error
        // if (value is Number) {
            if (value is Int) {
                expression(value)
            } else if (value is Double) {
                expression(value)
            } else if (value is Float) {
                expression(value)
            } else if (value is BigDecimal) {
                expression(value)
            } else if (value is Byte) {
                expression(value)
            } else if (value is Long) {
                expression(value)
            } else if (value is Short) {
                expression(value)
        /*
            } else {
                expression(value.toDouble())
            }
        */
        }
        else  if (value is String) {
            expression(value)
        } else  if (value is ByteArray) {
            expression(value)
        } else  if (value is Date) {
            expression(value)
        } else  if (value is Time) {
            expression(value)
        } else  if (value is Timestamp) {
            expression(value)
        } else {
            statement.setObject(nextParameterIndex(), value)
        }
    }

    fun expression(value : String?) : Unit {
        statement.setString(nextParameterIndex(), value)
    }

    fun expression(value : Int) : Unit {
        statement.setInt(nextParameterIndex(), value)
    }

    fun expression(value : BigDecimal?) : Unit {
        statement.setBigDecimal(nextParameterIndex(), value)
    }

    fun expression(value : Byte) : Unit {
        statement.setByte(nextParameterIndex(), value)
    }

    fun expression(value : Double) : Unit {
        statement.setDouble(nextParameterIndex(), value)
    }

    fun expression(value : Float) : Unit {
        statement.setFloat(nextParameterIndex(), value)
    }

    fun expression(value : Long) : Unit {
        statement.setLong(nextParameterIndex(), value)
    }

    fun expression(value : Short) : Unit {
        statement.setShort(nextParameterIndex(), value)
    }

    fun expression(value : ByteArray) : Unit {
        statement.setBytes(nextParameterIndex(), value)
    }

    fun expression(value : Date) : Unit {
        statement.setDate(nextParameterIndex(), value)
    }

    fun expression(value : Time) : Unit {
        statement.setTime(nextParameterIndex(), value)
    }

    fun expression(value : Timestamp) : Unit {
        statement.setTimestamp(nextParameterIndex(), value)
    }

    // TODO bind other kinds!

    /**
     * Looks up the [[PreparedStatement]] in a cache or creates a new one
     */
    protected fun lookupOrCreateStatement(): PreparedStatement {
        val answer = connection.prepareStatement(sql)
        if (answer == null) {
            throw IllegalStateException("No PreparedStatement returned from $connection")
        } else {
            return answer
        }
    }

    protected fun nextParameterIndex() : Int = ++parameterIndex

    protected fun createSql() : String {
        val out = StringBuilder()
        var constantText = true
        template.forEach {
            out.append(if (constantText) it else "?")
            constantText = !constantText
        }
        return out.toString() ?: ""
    }
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
