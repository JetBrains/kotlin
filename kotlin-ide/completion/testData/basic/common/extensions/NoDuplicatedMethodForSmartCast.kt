// FIR_COMPARISON

interface A {
    fun foo(): Int
}

interface B: A {
    override fun foo(): Int
    override fun foo1(): Int
}

fun g(a: A) {
    if (a is B) {
        a.fo<caret>
    }
}

// EXIST: foo
// EXIST: foo1
// NOTHING_ELSE
