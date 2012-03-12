package kotlin.jdbc

import java.sql.*

/**
 * Helper method to process a statement on this collection
 */
fun <T> Connection.statement(block: (Statement) -> T): T {
    val statement = createStatement()
    if (statement != null) {
        try {
            return block(statement)
        } finally {
            statement.close()
        }
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
 * Perform a query on the connection and processes the result set with a function
 */
fun <T> Connection.query(sql: String, block: (ResultSet) -> T): T {
    return statement{
        val rs = it.executeQuery(sql)
        if (rs != null) {
            block(rs)
        } else {
            throw IllegalStateException("No ResultSet returned executeQuery($sql) on $this")
        }
    }
}