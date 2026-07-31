fun main() {
    var caught = false
    try {
        throw RuntimeException("test")
    } catch (e: Throwable) {
        caught = true
    }
    check(caught) { "Expected catch block to execute" }
    println("OK")
}
