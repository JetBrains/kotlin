open class A

class <caret>B(a: Int): A() {
    // INFO: {"checked": "true"}
    val n: Int = a + 1
}