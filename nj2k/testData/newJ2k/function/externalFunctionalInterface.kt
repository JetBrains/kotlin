internal class Test {
    fun <A, B> foo(value: A, `fun`: FunctionalI<A, B>): B {
        return `fun`.apply(value)
    }

    fun toDouble(x: Int): Double {
        return x.toDouble()
    }

    fun nya(): Double {
        return foo(1, FunctionalI { x: Int -> this.toDouble(x) })
    }
}