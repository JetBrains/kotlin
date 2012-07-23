package kotlin.jdbc

/**
 * Helper method to process a statement on this collection
 */
import java.sql.Statement

/**
 * Uses the statement with the given block then closes the statement
 */
fun <T, S : Statement> S.useSql(block : (S) -> T) : T { // TODO rename to "use" when KT-2493 is fixed
    try {
        return block(this)
    } finally {
        close()
    }
}
