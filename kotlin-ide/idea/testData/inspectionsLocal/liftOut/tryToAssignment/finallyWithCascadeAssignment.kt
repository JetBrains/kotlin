// PROBLEM: none
fun doSomething() {}

fun test(n: Int) {
    var res: String? = null

    <caret>try {
        res = "success"
    } catch (e: Exception) {
        throw e
    } finally {
        if (n == 1)
            doSomething()
        else
            res = "finally"
    }
}