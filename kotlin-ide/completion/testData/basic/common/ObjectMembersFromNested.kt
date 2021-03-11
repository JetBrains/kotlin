// FIR_COMPARISON
object O {
    fun foo() {}

    class Nested {
        fun bar() {
            <caret>
        }
    }
}

// EXIST: foo
