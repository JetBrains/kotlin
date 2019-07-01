// EXPECTED_REACHABLE_NODES: 1274
// KJS_WITH_FULL_RUNTIME

import kotlin.js.json

fun box(): String {
    var obj = json(Pair("firstName", "John"), Pair("lastName", "Doe"), Pair("age", 30))
    assertEquals("John", obj["firstName"], "firstName")
    assertEquals("Doe", obj["lastName"], "lastName")
    assertEquals(30, obj["age"], "age")

    return "OK"
}