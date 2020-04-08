package asyncSimple

suspend fun main() {
    val a = 5
    foo()
    //Breakpoint!
    val b = a
}

suspend fun foo() {}