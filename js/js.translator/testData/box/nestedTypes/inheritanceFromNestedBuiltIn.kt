// EXPECTED_REACHABLE_NODES: 494
package foo

class EntryImplementor() : Map.Entry<String, String> {
    override val key: String
        get() = "foo"
    override val value: String
        get() = "bar"
}

fun box(): String {
    val entry = EntryImplementor()
    var stringResult = "${entry.key}${entry.value}"
    if (stringResult != "foobar") return "failed1: $stringResult"

    return "OK"
}