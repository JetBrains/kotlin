// OUT_OF_CODE_BLOCK: TRUE

class A {
    fun foo(): Int = 12
}

fun A.bar() = foo() + <caret>

// TYPE: 1
