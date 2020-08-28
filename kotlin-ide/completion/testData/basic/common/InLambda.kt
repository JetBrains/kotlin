// FIR_COMPARISON
fun foo(p: Iterable<D>) {
    p.filter { it.<caret> }
}

interface D {
    fun bar()
}

// EXIST: bar
