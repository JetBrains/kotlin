
fun foo(
    x1: Int,
    x2: Long,
    x3: Float,
    x4: Double
): Int {
    var mtmp1: Int = 0
    var mtmp2: Long = 0L
    var mtmp3: Float = 0f
    var mtmp4: Double = 0.0
    val tmp1 = x1
    val tmp2 = x2
    val tmp3 = x3
    val tmp4 = x4
    mtmp1 = x1
    mtmp2 = x2
    mtmp3 = x3
    mtmp4 = x4
    return mtmp1
}

var g1: Int = 0
var g2: Long = 0L
var g3: Float = 0f
var g4: Double = 0.0
fun fooUnit() {
    g1 = 10
    g2 = 10L
    g3 = 10f
    g4 = 10.0
    g1 = g1
    g2 = g2
    g3 = g3
    g4 = g4
}

fun box(): String {
    fooUnit()
    val res = foo(42, 100L, 100f, 100.0)
    if (res == 42)
        return "OK"
    return "Fail"
}