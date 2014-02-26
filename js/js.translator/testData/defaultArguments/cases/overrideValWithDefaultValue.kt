package foo

open class A(open val bar: Int = 2) {
    val barA = $bar
}

class B(override val bar: Int = 3) : A()


fun box(): String {
    val b = B()
    if (b.bar != 3) return "b.bar != 3, it: " + b.bar
    if (b.barA != 2) return "b.barA != 2, it: " + b.barA
    return "OK"
}