class Test {
    class Nested {
        class NestedNested
    }

    inner class Inner

    object NestedObject

    interface NestedInterface

    enum class NestedEnum {
        BLACK, WHITE
    }
}

class Foo {
    companion object Foo
}

class A {
    val x: A? = null

    fun f1(a: A, b: B): A? = null

    interface B {
        val y: B?

        fun f2(a: A, b: B): A? = null

        class A {
            val x: A? = null
            val y: B? = null

            fun f3(a: A, b: B) {}

            object B
        }
    }

    object C {
        interface C
    }
}

class A2 {
    class B {
        class C {
            class D {
                class A2
                class B
                class Cme
                class D
                class E
            }
        }
    }
}