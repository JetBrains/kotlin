abstract class QueryRunner(protected val queries: Queries) {
    fun performExampleQuery() {
        performQuery(queries.example)
    }

    fun performQuery(query: Query) {
    }
}