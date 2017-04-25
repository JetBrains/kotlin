// EXPECTED_REACHABLE_NODES: 492
// This test was adapted from compiler/testData/codegen/box/callableReference/property/.
package foo

val String.id: String
    get() = this

fun box(): String {
    val pr = String::id

    if (pr.get("123") != "123") return "Fail value: ${pr.get("123")}"

    if (pr.name != "id") return "Fail name: ${pr.name}"

    return pr.get("OK")
}
