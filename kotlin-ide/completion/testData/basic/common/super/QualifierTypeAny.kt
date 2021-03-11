interface I

class B : I {
    fun foo() {
        super<<caret>
    }
}

// EXIST: Any
// EXIST: I
// NOTHING_ELSE