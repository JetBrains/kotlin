// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/property/.
package foo

data class Box(var value: String)

fun box(): String {
    val o = Box("lorem")
    val prop = Box::value

    if (prop.get(o) != "lorem") return "Fail 1: ${prop[o]}"
    prop.set(o, "ipsum")
    if (prop.get(o) != "ipsum") return "Fail 2: ${prop[o]}"
    if (o.value != "ipsum") return "Fail 3: ${o.value}"
    o.value = "dolor"
    if (prop.get(o) != "dolor") return "Fail 4: ${prop.get(o)}"
    if ("$o" != "Box(value=dolor)") return "Fail 5: $o"

    return "OK"
}
