// FIX: Replace negated '!is' operation with 'is'
fun test(n: Int) {
    <caret>!(0 !is Int)
}