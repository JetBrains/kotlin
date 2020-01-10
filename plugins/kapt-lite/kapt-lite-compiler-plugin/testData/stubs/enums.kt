package enums

enum class Foo {
    FOO, BAR
}

enum class Foo2 {
    FOO {
        override val good = Good.GOOD
        override fun isGood() = true
    },
    BAR {
        override val good = Good.BAD
        override fun isGood() = false
    };

    abstract val good: Good
    abstract fun isGood(): Boolean

    fun baz(a: Int, b: String, c: Long) {}

    enum class Good {
        GOOD, BAD
    }
}

enum class Foo3 {
    FOO(5), BAR("");

    val n: Int
    val s: String

    constructor(n: Int) {
        this.n = n
        this.s = ""
    }

    constructor(s: String) {
        this.n = 0
        this.s = s
    }
}

enum class Foo4(val n: Int, val s: String) {
    FOO(1, "foo"), BAR(2, "bar")
}