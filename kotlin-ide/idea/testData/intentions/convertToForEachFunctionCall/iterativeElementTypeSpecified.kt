// WITH_RUNTIME
fun main() {
    val list = 1..4

    <caret>for (x: Int in list) {
        x
    }
}