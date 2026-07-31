fun exceptional(): Int = error("Test")

fun test() = try {
        exceptional()
    } catch (_: Throwable) {
        42
    }

fun main() {
    val value = test()
    check(value == 42) { "Expected 42 got $value" }
}
