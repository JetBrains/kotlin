package target

class A(val a: A, val a2: A) {
    val klass = A::class.java
    val klass2 = A::class.java
    val aa = A(a)
    val aa2 = A(a)
}