package test

inline fun f(body: () -> Unit) {
    println("i'm inline function")
    body()
}
