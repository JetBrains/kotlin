fun topLevel() = 1

class A(val prop: Int, arg: Int) {
    val another = 1
    constructor(abc: Int): this(1, 2) {
        val local = 1
        <caret>
    }

    fun foo() = 1
}

// EXIST: prop, abc, another, foo, local, topLevel
// ABSENT: arg
