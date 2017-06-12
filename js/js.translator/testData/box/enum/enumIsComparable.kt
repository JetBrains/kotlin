// EXPECTED_REACHABLE_NODES: 518
package foo

enum class A {
    one,
    two
}

fun box(): String {
    val x = A.one.compareTo(A.two)
    if (x != -1) return "Fail cmp(one, two) = $x"
    val y = A.two.compareTo(A.one)
    if (y != 1) return "Fail cmp(two, one) = $y"

    if (!(A.one < A.two)) return "Fail !(one < two)"
    if (A.one >= A.two) return "Fail one >= two"

    if (!(A.two > A.one)) return "Fail !(two > one)"
    if (A.two <= A.one) return "Fail two <= one"

    val z = A.one.compareTo(A.one)
    if (z != 0) return "Fail cmp(one, one) = $z"

    return "OK"
}