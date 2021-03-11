// Synthetic

class Synthetic {
    inner class Inner {
        fun test() {
            foo()
        }
    }

    private fun foo() {}
}

// FIR_COMPARISON