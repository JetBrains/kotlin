// WITH_RUNTIME
// PARAM_DESCRIPTOR: value-parameter it: kotlin.Int defined in foo.<anonymous>
// PARAM_TYPES: kotlin.Int
fun foo(): Int {
    1.let { <selection>return it + 1</selection> }
}