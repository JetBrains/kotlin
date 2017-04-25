// EXPECTED_REACHABLE_NODES: 497
package foo

class A
class B
class C

inline fun <reified T, reified R> test(x: Any): String = test1<R, T>(x)

inline fun <reified R, reified T> test1(x: Any): String =
    when (x) {
        is R -> "R"
        is T -> "T"
        else -> "Unknown"
    }

fun box(): String {
    assertEquals("T", test<A, B>(A()), "test<T, R>(T())")
    assertEquals("R", test<A, B>(B()), "test<T, R>(R())")
    assertEquals("Unknown", test<A, B>(C()), "test<T, R>(L())")

    return "OK"
}