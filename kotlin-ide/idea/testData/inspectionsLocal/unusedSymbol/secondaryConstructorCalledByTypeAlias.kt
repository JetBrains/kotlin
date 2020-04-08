// PROBLEM: none
class A(x: Double) {
    <caret>constructor(i: Int) : this(i.toDouble())
}

class C {
    val a = B(1)
}

typealias B = A