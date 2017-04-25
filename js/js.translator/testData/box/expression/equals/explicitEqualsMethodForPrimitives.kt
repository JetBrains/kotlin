// EXPECTED_REACHABLE_NODES: 489
package foo

fun box(): String {
    val a = 2
    if (!(a.equals(a))) return "fail1"
    if (!(a.equals(2))) return "fail2"
    if (!(a.equals(2.0))) return "fail3"
    val c = "a"
    if (!("a".equals(c))) return "fail4"
    if (!((null as Any?)?.equals(null) ?: true)) return "fail5"
    val d = 5.6
    if (!(d.toShort().equals(5.toShort()))) return "fail6"
    if (!(d.toByte().equals(5.toByte()))) return "fail7"
    if (!(d.toFloat().equals(5.6.toFloat()))) return "fail8"
    if (!(d.toInt().equals(5))) return "fail9"
    if (true.equals(false)) return "fail10"

    val n: Number = 3
    if (!(n.equals(3.3.toInt()))) return "fail11"
    return "OK"
}