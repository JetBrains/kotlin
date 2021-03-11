fun doSomething() {}

fun test(n: Int): String {
    <caret>try {
        return "success"
    } catch (e: Exception) {
        throw e
    } finally {
        if (n == 1)
            doSomething()
        else
            return "finally"
    }
}