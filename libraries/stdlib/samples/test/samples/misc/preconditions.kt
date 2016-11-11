package samples.misc

import samples.*
import kotlin.test.*

class Preconditions {

    @Sample
    fun failWithError() {
        val name: String? = null

        val exception = assertFailsWith<IllegalStateException> {
            val nonNullName = name ?: error("Name is missing")
        }

        assertPrints(exception.message, "Name is missing")
    }

    @Sample
    fun failRequireWithLazyMessage() {

        fun getIndices(count: Int) {
            require(count >= 0) { "Count must be non-negative, was $count" }
            // ...
        }

        val exception = assertFailsWith<IllegalArgumentException> {
            getIndices(-1)
        }
        assertPrints(exception.message, "Count must be non-negative, was -1")
    }

    @Sample
    fun failCheckWithLazyMessage() {

        var someState: String? = null
        fun getStateValue(): String {
            val state = checkNotNull(someState) { "State must be set beforehand" }
            check(state.isNotEmpty()) { "State must be non-empty" }
            // ...
            return state
        }

        assertFailsWith<IllegalStateException> {
            getStateValue()
        }

        assertFailsWith<IllegalStateException> {
            someState = ""
            getStateValue()
        }.let { exception ->
            assertPrints(exception.message, "State must be non-empty")
        }
    }
}
