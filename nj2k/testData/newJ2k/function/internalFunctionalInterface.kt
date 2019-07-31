// ERROR: Unresolved reference: A
// ERROR: Interface FunctionalI does not have constructors
internal interface FunctionalI<A, B> {
    fun apply(x: A): B
}

internal class Test {
    fun <A, B> foo(value: A, `fun`: FunctionalI<A, B>): B {
        return `fun`.apply(value)
    }

    fun toDouble(x: Int): Double {
        return x.toDouble()
    }

    fun nya(): Double {
        return foo(1, FunctionalI<Int?, Double?> { x: A -> this.toDouble(x) })
    }
}