package inline

fun doo(fn: () -> Unit) = fn()

inline fun f() {
    doo {
        println("i'm inline function")
    }
}