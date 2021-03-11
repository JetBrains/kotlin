package inline

inline fun f(body: () -> Unit) {
    println("i'm inline function")
    body()
}

class Klass {
    inline fun f(body: () -> Unit) {
        println("i'm inline function")
        body()
    }
}
