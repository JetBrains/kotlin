interface DelegateFace
fun compoundDelegate(): DelegateFace {
    val result = object : DelegateFace {}
    return result
}
class DelegatingB : DelegateFace by <caret>compoundDelegate()