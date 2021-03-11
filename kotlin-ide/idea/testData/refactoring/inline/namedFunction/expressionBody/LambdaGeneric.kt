fun <T, U> operateLambda(p: T, f: (T) -> U) = f(p)
fun returnLabel() {
    val a = <caret>operateLambda(1) {}
}