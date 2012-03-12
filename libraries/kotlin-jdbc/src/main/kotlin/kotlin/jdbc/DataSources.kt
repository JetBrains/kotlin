package kotlin.jdbc

import java.sql.*
import javax.sql.*

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
    return use{ it.statement(block) }
}

/**
 * Perform an SQL update on the connection
 */
fun DataSource.update(sql: String): Int {
    return use{ it.update(sql) }
}

/**
 * Perform a query on the connection and processes the result set with a function
 */
fun <T> DataSource.query(sql: String, block: (ResultSet) -> T): T {
    return use{ it.query(sql, block) }
}