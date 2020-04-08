// WITH_RUNTIME
// PARAM_TYPES: kotlin.Int, kotlin.Comparable<kotlin.Int>
// PARAM_DESCRIPTOR: value-parameter i: kotlin.Int defined in example
fun example(i: Int) {
    when (i) {
        1 -> {
            <selection>if (i > 5) {
                println("true")
            }
            else {
                println("false")
                return
            }
            println("!!")</selection>
        }
    }
}