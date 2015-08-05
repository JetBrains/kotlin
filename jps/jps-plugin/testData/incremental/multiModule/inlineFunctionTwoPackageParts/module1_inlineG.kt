package inline

inline fun g(body: () -> Unit) {
    println("i'm inline function")
    body()
}
