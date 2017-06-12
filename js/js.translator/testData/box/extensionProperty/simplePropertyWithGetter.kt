// EXPECTED_REACHABLE_NODES: 489
package foo

val String.prop: Int
    get() = length

val Int.quadruple: Int
    get() = this * 4

fun box(): String {
    if ("1".prop != 1) return "fail1";
    if ("11".prop != 2) return "fail2";
    if (("121" + "123").prop != 6) return "fail3";
    if (1.quadruple != 4) return "fail4";
    if (0.quadruple != 0) return "fail5";
    return "OK";
}