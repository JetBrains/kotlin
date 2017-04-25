// EXPECTED_REACHABLE_NODES: 500
// This test was adapted from compiler/testData/codegen/box/callableReference/property/.
package foo

data class Box(val value: String)

var pr = Box("first")

fun box(): String {
    val property = ::pr
    if (property.get() != Box("first")) return "Fail value: ${property.get()}"
    if (property.name != "pr") return "Fail name: ${property.name}"
    property.set(Box("second"))
    if (property.get().value != "second") return "Fail value 2: ${property.get()}"
    return "OK"
}
