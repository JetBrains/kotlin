// EXPECTED_REACHABLE_NODES: 1344
open class MyClass1 private constructor(val value: String) {
    constructor(i: Int): this(i.toString())
}

class MyClass2 : MyClass1 {
    constructor(i: Int): super(i)
}

fun test(x: Any) = x is MyClass2

fun box(): String {
    val b = test(MyClass2(0))

    return if (b) "OK" else "NOT OK"
}