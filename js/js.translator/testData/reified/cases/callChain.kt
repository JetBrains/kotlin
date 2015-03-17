package foo

// CHECK_NOT_CALLED: test
// CHECK_NOT_CALLED: test1

class A
class B

inline fun test<reified T>(x: Any): Boolean = test1<T>(x)

inline fun test1<reified R>(x: Any): Boolean = x is R

fun box(): String {
    assertEquals(true, test<A>(A()), "test<A>(A())")
    assertEquals(false, test<A>(B()), "test<A>(B())")

    return "OK"
}