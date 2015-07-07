package inline

inline fun f(body: () -> Unit) {
    body()
}
