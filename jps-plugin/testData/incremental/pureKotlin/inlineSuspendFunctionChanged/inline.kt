package inline

inline suspend fun f(body: () -> Unit) {
    println("i'm inline suspend function")
    body()
}
