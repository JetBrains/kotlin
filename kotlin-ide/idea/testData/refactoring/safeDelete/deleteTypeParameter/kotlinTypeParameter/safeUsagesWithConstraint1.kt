class A<out <caret>X: Comparable<X>, in Y: Number, Z>

val a = A<String, Int, Any>()