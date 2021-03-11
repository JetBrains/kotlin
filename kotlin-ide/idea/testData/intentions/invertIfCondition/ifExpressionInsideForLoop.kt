// WITH_RUNTIME
fun main() {
    val list = 1..4

    for (x in list) {
        <caret>if (x == 2) {
            list
        }
    }
}