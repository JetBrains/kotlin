class Leaf : Node3() { val w: Int = 100 }

fun leaf(): Int { val l = Leaf(); return l.bx() + l.v1 + l.v2 + l.v3 + l.w }
