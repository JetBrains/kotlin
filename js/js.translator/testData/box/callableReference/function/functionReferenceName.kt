// EXPECTED_REACHABLE_NODES: 1221
package foo

import kotlin.reflect.KFunction

fun foo0() = "OK"
fun foo1(a: String) = "O" + a
fun foo2(a: String, b: String) = a + b

fun refName0(ref: KFunction<String>) = ref.name

fun box(): String {
    val name = refName0(::foo0)
    val f1 = ::foo1
    val f2 = ::foo2

    if (name != "foo0") return "Fail: " + name
    if (f1.name != "foo1") return "Fail: " + f1.name
    if (f2.name != "foo2") return "Fail: " + f2.name
    return "OK"
}
