// ERROR: Interface FunctionalI does not have constructors
internal class Test {
    fun <A, B> foo(value: A, `fun`: FunctionalI<A, B>): B {
        return `fun`.apply(value)
    }

    fun toDouble(x: Int): Double {
        return x.toDouble()
    }

    fun nya(): Double {
        //TODO explicitlly call apply here

        return foo(1, FunctionalI<Int?, Double?> { x: Int -> this.toDouble(x) })
    }
}