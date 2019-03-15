// EXPECTED_REACHABLE_NODES: 1281
package foo

fun <T> T.toPrefixedString(prefix: String = "", suffix: String = "") = prefix + toString() + suffix

fun box(): String {
    return if (("mama".toPrefixedString(suffix = "321", prefix = "papa") == "papamama321")) "OK" else "fail"
}
