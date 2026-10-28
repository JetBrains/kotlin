class ShortLeaf : Node1() { val u: Int = 1000 }

fun shortLeaf(): Int { val l = ShortLeaf(); return l.bx() + l.v1 + l.u }
