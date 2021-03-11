annotation class foo(val n: Int)

class <caret>Test(@foo(1) @foo(2) @foo(3) private val s: String)