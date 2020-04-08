import kotlin.Nothing as MyNothing

fun myNothingFun(): MyNothing = throw Exception()

fun foo(p: Any) {
    if (p !is String) {
        myNothingFun()
    }
    println(<caret>p.length())
}