open class A

class <caret>B: A {
    // INFO: {"checked": "true"}
    val n: Int

    // INFO: {"checked": "true"}
    val x: Int = 3
    // INFO: {"checked": "false"}
    val y: Int = 4

    // INFO: {"checked": "true"}
    val a: Int = 1
    // INFO: {"checked": "true"}
    val b: Int = x + y

    constructor(p: Int) {
        n = a + b - p
    }
}