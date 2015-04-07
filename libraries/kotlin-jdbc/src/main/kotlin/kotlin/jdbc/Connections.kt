package kotlin.jdbc

import java.sql.*
import kotlin.template.StringTemplate
import java.math.BigDecimal
import java.util.Properties

/**
 * create connection for the specified jdbc url with no credentials
 */
fun getConnection(url : String) : Connection = DriverManager.getConnection(url)

/**
 * create connection for the specified jdbc url and properties
 */
fun getConnection(url : String, info : Map<String, String>) : Connection = DriverManager.getConnection(url, info.toProperties())

/**
 * create connection for the specified jdbc url and credentials
 */
fun getConnection(url : String, user : String, password : String) : Connection = DriverManager.getConnection(url, user, password)

/**
 * Executes specified block with connection and close connection after this
 */
fun <T> Connection.use(block : (Connection) -> T) : T {
    try {
        return block(this)
    } finally {
        this.close()
    }
}

/**
 * Helper method to process a statement on this connection
 */
fun <T> Connection.statement(block: (Statement) -> T): T {
    val statement = createStatement()
    if (statement != null) {
        return statement.use(block)
    } else {
        throw IllegalStateException("No Statement returned from $this")
    }
}

/**
 * Perform an SQL update on the connection
 */
fun Connection.update(sql: String): Int {
    return statement{ it.executeUpdate(sql) }
}

/**
 * Performs the SQL update using the [[StringTemplate]]
 */
fun Connection.update(template : StringTemplate) : Int {
    val preparedStatement = prepare(template)
    return preparedStatement.update()
}


/**
 * Perform a query on the connection and processes the result set with a function
 */
fun <T> Connection.query(sql: String, block: (ResultSet) -> T): T {
    return statement{
        val rs = it.executeQuery(sql)
        block(rs)
    }
}



/**
 * Perform a query on the connection using the [[StringTemplate]] to generate the SQL text
 * and processes the result set with a function
 */
fun <T> Connection.query(template : StringTemplate, resultBlock : (ResultSet) -> T) : T {
    val preparedStatement = prepare(template)
    return preparedStatement.query(resultBlock)
}

/**
 * Creates a [[PreparedStatement]] from the [[StringTemplate]]
 */
fun Connection.prepare(template : StringTemplate) : PreparedStatement {
    val builder = PreparedStatementBuilder(template, this)
    builder.bind()
    return builder.statement
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
        return out.toString()
    }
}

