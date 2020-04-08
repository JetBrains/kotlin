// ERROR: A 'return' expression required in a function with a block body ('{...}')
// PROBLEM: none
fun doSomething() {}

fun test(): String {
    <caret>try {
        return "success"
    } catch (e: Exception) {
        doSomething()
    }
}