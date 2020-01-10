package javadoc

/** Foo */
class Foo /** primary ctor */ constructor(/** x */ x: Int, y: Int) {
    /** secondary ctor */
    constructor(/** only x */ x: Int) : this(x, 0)

    /** foo() */
    fun foo(/** a */ a: Int) {}

    /** prop */
    val prop: Int = 0

    var prop2: Int
        /** prop2/get */ get() = 0
        /** prop2/set */ set(/** sparam */ value) {}

    /** Nested */
    class Nested {}

    /** Inner */
    inner class Inner {
        /** u */
        fun u() {}
    }

    // simple comment
    fun comment() {}

    /* block comment */
    fun blockComment() {}
}

/** FooEnum */
enum class FooEnum {
    /** FOO */ FOO {
        /** FOO/foo */
        override fun foo(a: Int) {}
    },
    /** BAR */ BAR {
        override fun foo(a: Int) {}
    };

    /** FooEnum/foo */
    abstract fun foo(/** a */ a: Int)
}

/** Anno */
annotation class Anno(/** Anno/value */ val value: String, /** Anno/x */ val x: Int)

/** topLevel */
fun topLevel() {}

/** topLevelExt */
fun String.topLevelExt() {}

/** topLevelConst */
const val topLevelConst: Int = 0

/** topLevelExtProperty */
val String.topLevelExtProperty: Int
    /** topLevelExtProperty/get */ get() = 0

/**
 * `/* Failure */`
 */
interface TestComponent