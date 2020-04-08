fun <R> bar(f: () -> R) = f()

fun test() {
    bar {
        return@bar false
    }
}