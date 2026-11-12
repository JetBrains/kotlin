interface StatementInterceptor {
    /** Performs steps after commit. */
    fun afterCommit(transaction: String) {}
}

/** Global interceptor. */
interface GlobalStatementInterceptor : StatementInterceptor
