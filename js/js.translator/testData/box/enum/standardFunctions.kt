// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1555
package foo

enum class EmptyEnum

enum class A {
    a() {
    },
    b(),
    c
}

fun box(): String {
    if (enumValues<EmptyEnum>().size != 0) return "enumValues<EmptyEnum>().size != 0"
    if (enumValues<A>().asList() != listOf(A.a, A.b, A.c)) return "Wrong enumValues<A>(): " + enumValues<A>().toString()
    if (enumValueOf<A>("b") != A.b) return "enumValueOf<A>('b') != A.b"
    return "OK"
}