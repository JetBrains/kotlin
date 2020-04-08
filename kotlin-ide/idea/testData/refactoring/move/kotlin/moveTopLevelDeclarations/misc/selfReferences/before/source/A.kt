package source

class <caret>A(val a: A, val a2: source.A) {
    val klass = A::class.java
    val klass2 = source.A::class.java
    val aa = A(a)
    val aa2 = source.A(a)
}

class B {

}