// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in bar
// PARAM_DESCRIPTOR: value-parameter b: kotlin.Int defined in bar
fun bar(a: Int, b: Int) {
    val foo = { <selection>a + b</selection> - 1 }.invoke()
}