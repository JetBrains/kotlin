package kotlin.jdbc

import java.sql.*
import javax.sql.*
import kotlin.template.StringTemplate

/**
 * Processes a connection from the pool using the given function block
 */
fun <T> DataSource.use(block : (Connection) -> T): T {
    val connection = getConnection()
    if (connection != null) {
        try {
            return block(connection)
        } finally {
            connection.close()
        }
    } else {
        throw IllegalStateException("No Connection returned from $this")
    }
}

/**
 * Helper method to process a statement on this collection
 */
fun <T> DataSource.statement(block: (Statement) -> T): T {
    return use { it.statement(block) }
}

/**
 * Perform an SQL update on the connection
 */
fun DataSource.update(sql: String): Int {
    return use { it.update(sql) }
}

/**
 * Perform a query on the connection and processes the result set with a function
 */
fun <T> DataSource.query(sql: String, block: (ResultSet) -> T): T {
    return use { it.query(sql, block) }
}

/**
 * Performs the update using the given SQL using a [[StringTemplate]]
 */
fun DataSource.update(template : StringTemplate) : Int {
    return use { it.update(template) }
}

/**
 * Perform a query on the connection using the SQL from the [[StringTemplate]] and processes the result set with a function
 */
fun <T> DataSource.query(template : StringTemplate, resultBlock : (ResultSet) -> T) : T {
    return use { it.query(template, resultBlock) }
}
