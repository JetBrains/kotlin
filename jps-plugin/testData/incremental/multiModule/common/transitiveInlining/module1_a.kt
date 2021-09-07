package a

inline fun a(body: () -> Unit) {
    println("i'm inline function")
    body()
}
