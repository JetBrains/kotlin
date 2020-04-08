class A {
    public fun foo(): Int = 1
    public val bar: Int = 1
}

val A.abc: Int
    get() {
        <caret>
    }

// EXIST: foo
// EXIST: bar
