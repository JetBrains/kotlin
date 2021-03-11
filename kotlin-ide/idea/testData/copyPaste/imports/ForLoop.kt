package a

class A() {
}

class B() {
}

operator fun B.next(): Int = 3

operator fun B.hasNext(): Boolean = false

operator fun A.iterator() = B()

<selection>fun f() {
    for (i in A()) {
    }
}</selection>