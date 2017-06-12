// EXPECTED_REACHABLE_NODES: 488
package foo

fun <T> T.toPrefixedString(prefix: String = "", suffix: String = "") = prefix + toString() + suffix

fun box(): String {
    if ("mama".toPrefixedString(suffix = "321", prefix = "papa") != "papamama321") return "fail1"
    if ("mama".toPrefixedString(prefix = "papa") != "papamama") return "fail2"
    if ("mama".toPrefixedString("papa", "239") != "papamama239") return "fail3"
    if ("mama".toPrefixedString("papa") != "papamama") return "fail4"
    if ("mama".toPrefixedString() != "mama") return "fail5"
    return "OK"
}
