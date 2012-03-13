package kotlin.jdbc

/**
 * Helper method to process a statement on this collection
 */
import java.sql.Statement

/**
 * Uses the statement with the given block then closes the statement
 */
fun <T, S : Statement> S.use(block : (S) -> T) : T {
    try {
        return block(this)
    } finally {
        close()
    }
}
