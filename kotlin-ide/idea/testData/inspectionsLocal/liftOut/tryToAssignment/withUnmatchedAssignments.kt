// PROBLEM: none
fun test() {
    var res: String? = null
    var foo: String? = null

    <caret>try {
        res = "success"
    } catch (e: Exception) {
        foo = "exception"
    }
}