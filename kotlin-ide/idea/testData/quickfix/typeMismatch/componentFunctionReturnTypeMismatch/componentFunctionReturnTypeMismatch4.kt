// "Change return type of called function 'A.component2' to 'Unit'" "true"
// ERROR: The integer literal does not conform to the expected type Unit
abstract class A {
    abstract operator fun component1(): Int
    operator fun component2() = 42
}

fun foo(a: A) {
    val (w: Int, x: Unit) = a<caret>
}