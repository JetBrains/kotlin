// OUT_OF_CODE_BLOCK: TRUE

class A {
    fun foo(): Int = 12

    fun bar(): Int = foo() + <caret>
}

// TYPE: 1
// TODO
// SKIP_ANALYZE_CHECK