annotation class foo(val n: Int)

class Test(@foo(1) @foo(2) @foo(3) private val s: String, n: Int)