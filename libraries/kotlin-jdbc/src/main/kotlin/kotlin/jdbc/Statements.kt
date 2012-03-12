package kotlin.jdbc

/**
 * Helper method to process a statement on this collection
 */
import java.sql.Statement

/**
 * Uses the statement with the given block then closes the statement
 */
fun <T> Statement.use(block : (Statement) -> T) : T {
    try {
        return block(this)
    } finally {
        close()
    }
}
