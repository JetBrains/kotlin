// WITH_RUNTIME
fun main() {
    outer@
    <caret>for (i in 1..100) {
        if (i % 2 == 0) continue
        inner@
        for (j in 1..100) {
            continue@inner
        }
        for (j in 1..100) {
            for (k in 1..1) {
                continue@outer
            }
            continue
        }
    }
}