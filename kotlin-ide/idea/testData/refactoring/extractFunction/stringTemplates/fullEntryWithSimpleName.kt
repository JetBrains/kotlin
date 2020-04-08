// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// PARAM_TYPES: kotlin.Int, kotlin.Number, kotlin.Comparable<kotlin.Int>, java.io.Serializable, kotlin.Any
fun foo(a: Int): String {
    val x = "-$a"
    val y = "x${a}y"
    val z = "x$ay"
    return "abc<selection>${a}</selection>def"
}