// EXPECTED_REACHABLE_NODES: 496
package foo

open class A(val name: String)

class B(val age: Int, name: String) : A(name)

fun box(): String {
    val b = B(12, "Mike")

    if (b.age != 12) return "b.age != 12, it: ${b.age}"
    if (b.name != "Mike") return "b.name != 'Mike', it: ${b.name}"

    return "OK"
}