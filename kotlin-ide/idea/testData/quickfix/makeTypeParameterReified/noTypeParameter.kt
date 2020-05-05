// "Make type parameter reified and function inline" "false"
// ACTION: Change type arguments to <*>
// ACTION: Convert to block body
// ACTION: Introduce local variable
// ACTION: Expand boolean expression to 'if else'
// ERROR: Cannot check for instance of erased type: List<Int>
fun test(a: List<Any>) = a is List<Int><caret>