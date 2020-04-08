// OPTION: 1
fun foo(n: Int): Int {
    try {
        n / 0
    }
    catch (e: ArithmeticException) {
        val m = -1
        m
    }
    catch (e: Exception) {
        -2
    }
    <caret>finally {
        println("finally")
    }

    return 0
}
