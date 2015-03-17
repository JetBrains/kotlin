package foo

class A
class B
class C

inline fun test<reified T, reified R>(x: Any): String = test1<R, T>(x)

inline fun test1<reified R, reified T>(x: Any): String =
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