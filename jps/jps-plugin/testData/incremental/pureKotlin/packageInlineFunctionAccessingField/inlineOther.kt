package other

val property = ":)"

inline fun f(body: () -> Unit) {
    println("i'm inline function" + property)
    body()
}
