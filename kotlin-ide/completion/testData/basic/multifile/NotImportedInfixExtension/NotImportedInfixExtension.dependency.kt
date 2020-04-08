package other

import pack.C

fun C.xxx(p: Int) {}
infix fun C.yyy(p: Int) {}
fun C.zzz(p1: Int, p2: Int) {}
val C.extensionProp: Int get() = 1

// ALLOW_AST_ACCESS
