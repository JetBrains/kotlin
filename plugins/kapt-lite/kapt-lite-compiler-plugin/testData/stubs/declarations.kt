package declarations

class Foo {
    val a: Int = 0
    fun foo() {}
    fun bar(str: String, num: Int) {}

    class Nested {
        fun nested() {}

        class Nested2 {
            fun nested2() {}
        }
    }

    inner class Inner {
        fun inner() {}

        inner class Inner2 {
            fun inner2() {}
        }
    }

    interface NestedIntf {
        fun nestedIntf(x: Int) {}
    }

    enum class NestedEnum {
        GOOD, BAD
    }

    annotation class Anno(val value: String)
}

interface Intf {
    val a: Int
    fun foo() {}
    fun bar(str: String, num: Int)

    fun fooImpl(a: Int, b: String) {}
    suspend fun barImpl(a: Int, b: String) {}
}

annotation class Anno(val a: String, val b: Int, val c: IntArray)
annotation class ValueAnno(val value: String)

enum class FooEnum {
    FOO, BAR
}

enum class FooEnum2 {
    FOO, BAR;

    enum class NestedEnum {
        GOOD, BAD
    }
}

object Obj

class ClassWithCompanion {
    companion object {
        fun foo() {}
    }
}

class ClassWithNamedCompanion {
    companion object Com {
        fun foo() {}
    }
}

class InterfaceWithCompanion {
    companion object {
        fun foo() {}
    }
}

class InterfaceWithNamedCompanion {
    companion object ComIntf {
        fun foo() {}
    }
}