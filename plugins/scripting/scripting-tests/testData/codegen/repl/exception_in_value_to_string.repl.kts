// SNIPPET

class B {
    override fun toString(): String = foo()
    fun foo(): String = error("message")
}

// SNIPPET

B().toString()

// EXPECTED_EXCEPTION: java.lang.IllegalStateException: message
