import java.util.*

object Test {
    private fun <T> <caret>foo(): List<T> {
        return emptyList<T>()
    }

    private fun <T> bar(xs: List<T>): List<T> {
        return xs
    }

    private fun gazonk() {
        val ss = bar(Test.foo<String>())
    }
}