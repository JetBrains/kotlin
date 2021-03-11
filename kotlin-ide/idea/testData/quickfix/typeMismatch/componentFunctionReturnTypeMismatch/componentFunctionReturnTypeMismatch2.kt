// "Change return type of called function 'A.component2' to 'Int'" "true"
abstract class A {
    abstract operator fun component1(): Int
    abstract operator fun component2(): String
}

fun foo(a: A) {
    val (w: Int, x: Int) = a<caret>
}