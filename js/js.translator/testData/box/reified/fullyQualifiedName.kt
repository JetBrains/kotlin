// EXPECTED_REACHABLE_NODES: 1503
package foo

// CHECK_CALLED: f2
// CHECK_NOT_CALLED: f1
// CHECK_NOT_CALLED: f3
// CHECK_NOT_CALLED: f4

class F1
class F2
class F3
class F4

inline fun <reified T> f1(): String {
    return T::class.qualifiedName ?: "null"
}

fun f2(): String {
    return F2::class.qualifiedName ?: "null"
}

inline fun <reified T : Any> f3(instance: kotlin.reflect.KClass<T>): String {
    return instance.qualifiedName ?: "null"
}

inline fun <reified T : Any> kotlin.reflect.KClass<T>.f4(): String {
    return qualifiedName ?: "null"
}

fun box(): String {
    assertEquals("foo.F1", f1<F1>(), "T::class.qualifiedName")
    assertEquals("foo.F2", f2(), "A::class.qualifiedName")
    assertEquals("foo.F3", f3<F3>(F3::class), "instance.qualifiedName")
    assertEquals("foo.F4", F4::class.f4(), "instance.qualifiedName")

    return "OK"
}