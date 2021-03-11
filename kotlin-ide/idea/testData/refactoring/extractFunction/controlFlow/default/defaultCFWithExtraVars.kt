// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: val b: kotlin.Int defined in foo
// WITH_RUNTIME
// SIBLING:
fun foo(a: Int) {
    val b: Int = 1

    <selection>if(a > 0) {
        println(a)
    }
    val c: Int
    println(b)</selection>

    c = 1
    println(c)
}