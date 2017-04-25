// EXPECTED_REACHABLE_NODES: 941
package foo

enum class EmptyEnum

enum class Simple {
    OK
}

enum class A {
    a() {
    },
    b(),
    c
}

enum class B {
    c
}
fun box(): String {
    if (Simple.OK.name != "OK") return "Simple.OK.name != OK, it: ${Simple.OK.name}"
    val ok = Simple.OK
    if (ok.ordinal != 0) return "ok = Simple.Ok; ok.ordinal != 0, it: ${ok.ordinal}"

    val ok2 = Simple.valueOf("OK")
    if (!ok2.equals(ok)) return "ok2 not equal ok"
    if (ok2.hashCode() != ok.hashCode()) return "hash(ok2) not equal hash(ok)"
    if (ok2 !== ok) return "ok2 not identity equal ok"


    if (EmptyEnum.values().size != 0) return "EmptyEnum.values().size != 0"

    if (A.values().asList() != listOf(A.a, A.b, A.c)) return "Wrong A.values(): " + A.values().toString()

    if (A.c.toString() != "c") return "A.c.toString() != c, it: ${A.c.toString()}"
    if (A.valueOf("b") != A.b) return "A.valueOf('b') != A.b"
    if (A.a == A.b) return "A.a == A.b"
    if (A.a.hashCode() == A.b.hashCode()) return "hash(A.a) == hash(A.b)"

    if (A.a.name != "a") return "A.a.name != a, it: ${A.a.name}"
    if (A.b.name != "b") return "A.b.name != b, it: ${A.b.name}"
    if (A.c.name != "c") return "A.c.name != c, it: ${A.c.name}"

    if (A.a.ordinal != 0) return "A.a.ordinal != 0, it: ${A.a.ordinal}"
    if (A.b.ordinal != 1) return "A.b.ordinal != 1, it: ${A.b.ordinal}"
    if (A.c.ordinal != 2) return "A.c.ordinal != 2, it: ${A.c.ordinal}"

    if (A.c.equals(B.c)) return "A.c.equals(B.c)"

    return "OK"
}