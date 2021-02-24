package samples.misc

import samples.*
import kotlin.test.*

class Preconditions {

    @Sample
    @Suppress("UNUSED_VARIABLE")
    fun failWithError() {
        val name: String? = null

        assertFailsWith<IllegalStateException> { val nonNullName = name ?: error("Name is missing") }
    }

    @Sample
    fun failRequireWithLazyMessage() {

        fun getIndices(count: Int): List<Int> {
            require(count >= 0) { "Count must be non-negative, was $count" }
            // ...
            return List(count) { it + 1 }
        }

        assertFailsWith<IllegalArgumentException> { getIndices(-1) }

        assertPrints(getIndices(3), "[1, 2, 3]")
    }

    @Sample
    fun failRequireNotNullWithLazyMessage() {

        fun printRequiredParam(params: Map<String, String?>) {
            val required: String = requireNotNull(params["required"]) { "Required value must be non-null" } // returns a non-null value
            println(required)
            // ...
        }

        fun printRequiredParamByUpperCase(params: Map<String, String?>) {
            val requiredParam: String? = params["required"]
            requireNotNull(requiredParam) { "Required value must be non-null" }
            // now requiredParam is smartcast to String so that it is unnecessary to use the safe call(?.)
            println(requiredParam.toUpperCase())
        }

        val params: MutableMap<String, String?> = mutableMapOf("required" to null)
        assertFailsWith<IllegalArgumentException> { printRequiredParam(params) }
        assertFailsWith<IllegalArgumentException> { printRequiredParamByUpperCase(params) }

        params["required"] = "non-empty-param"
        printRequiredParam(params) // prints "non-empty-param"
        printRequiredParamByUpperCase(params) // prints "NON-EMPTY-PARAM"
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

        assertFailsWith<IllegalStateException> { getStateValue() }

        someState = ""
        assertFailsWith<IllegalStateException> { getStateValue() }

        someState = "non-empty-state"
        assertPrints(getStateValue(), "non-empty-state")
    }
}
