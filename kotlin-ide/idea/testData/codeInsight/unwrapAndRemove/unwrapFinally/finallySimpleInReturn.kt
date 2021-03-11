// IS_APPLICABLE: false
fun foo(n: Int): Int {
    return try {
        n / 0
    }
    catch (e: ArithmeticException) {
        -1
    }
    catch (e: Exception) {
        -2
    }
    <caret>finally {
        println("finally")
    }
}
