// OUT_OF_CODE_BLOCK: TRUE

// as it's a object body not a named function body

object A {
    fun foo(): Int = 12

    private fun bar(): Int = foo() + <caret>
}

// TYPE: 1
// SKIP_ANALYZE_CHECK