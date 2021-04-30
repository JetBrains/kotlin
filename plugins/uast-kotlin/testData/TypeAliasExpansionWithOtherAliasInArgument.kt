// !IGNORE_FIR

typealias A = String
typealias My = (Map<A, Int>) -> Unit

fun My.foo(x: My): My? = null
fun Map<A, Int>.bar(x: Map<A, Int>): Map<A, Int>? = null
