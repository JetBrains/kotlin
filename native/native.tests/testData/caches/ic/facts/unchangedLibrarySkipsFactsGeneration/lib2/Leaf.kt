class Leaf : Base() { val w: Int = 100 }

fun leaf(): Int { val l = Leaf(); return l.bx() + l.w }
