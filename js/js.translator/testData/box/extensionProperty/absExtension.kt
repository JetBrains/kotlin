// EXPECTED_REACHABLE_NODES: 488
package foo

val Double.abs: Double
    get() = if (this > 0) this else -this

fun box(): String {
    if (4.0.abs != 4.0) return "fail1";
    if ((-5.2).abs != 5.2) return "fail2";

    return "OK";
}
