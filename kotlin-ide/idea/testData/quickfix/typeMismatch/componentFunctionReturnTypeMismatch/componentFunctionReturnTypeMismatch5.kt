// "Change type of 'y' to 'Int'" "true"
class A {
    operator fun component1() = 42
    operator fun component2() = 42
    operator fun component3() = 42
}

fun foo(a: A) {
    val (x: Int, y: String, z: Int) = a<caret>
}