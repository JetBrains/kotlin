// PROBLEM: none
val foo: String?<caret>
    get() {
        val s = bar() ?: return null
        return s
    }

fun bar(): String? = null